/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.SendFailedException;
import javax.mail.internet.*;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.services.MailServer;
import org.apache.james.services.MailStore;
import org.apache.james.services.SpoolRepository;
import org.apache.james.transport.*;
import org.apache.mailet.*;

/**
 * Receive  a MessageContainer from JamesSpoolManager and takes care of delivery
 * the message to remote hosts. If for some reason mail can't be delivered
 * store it in the "outgoing" Repository and set an Alarm. After "delayTime" the
 * Alarm will wake the servlet that will try to send it again. After "maxRetries"
 * the mail will be considered underiverable and will be returned to sender.
 *
 * TO DO (in priority):
 * 1. Support a gateway (a single server where all mail will be delivered) (DONE)
 * 2. Provide better failure messages (DONE)
 * 3. More efficiently handle numerous recipients
 * 4. Migrate to use Phoenix for the delivery threads
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 *
 * This is $Revision: 1.12 $
 * Committed on $Date: 2001/12/07 23:28:34 $ by: $Author: serge $
 */
public class RemoteDelivery extends GenericMailet implements Runnable {

    private SpoolRepository outgoing;
    private long delayTime = 21600000; // default is 6*60*60*1000 millis (6 hours)
    private int maxRetries = 5; // default number of retries
    private long smtpTimeout = 600000;  //default number of ms to timeout on smtp delivery
    private int deliveryThreadCount = 1; // default number of delivery threads
    private String gatewayServer = null; // the server to send all email to
    private String gatewayPort = null;  //the port of the gateway server to send all email to
    private Collection deliveryThreads = new Vector();
    private MailServer mailServer;

    public void init() throws MessagingException {
        try {
            if (getInitParameter("delayTime") != null) {
                delayTime = Long.parseLong(getInitParameter("delayTime"));
            }
        } catch (Exception e) {
            log("Invalid delayTime setting: " + getInitParameter("delayTime"));
        }
        try {
            if (getInitParameter("maxRetries") != null) {
                maxRetries = Integer.parseInt(getInitParameter("maxRetries"));
            }
        } catch (Exception e) {
            log("Invalid maxRetries setting: " + getInitParameter("maxRetries"));
        }
        try {
            if (getInitParameter("timeout") != null) {
                smtpTimeout = Integer.parseInt(getInitParameter("timeout"));
            }
        } catch (Exception e) {
            log("Invalid timeout setting: " + getInitParameter("timeout"));
        }
        gatewayServer = getInitParameter("gateway");
        gatewayPort = getInitParameter("gatewayPort");
        ComponentManager compMgr = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        String outgoingPath = getInitParameter("outgoing");
        if (outgoingPath == null) {
            outgoingPath = "file:///../var/mail/outgoing";
        }

        try {
            // Instantiate the a MailRepository for outgoing mails
            MailStore mailstore = (MailStore) compMgr.lookup("org.apache.james.services.MailStore");

            DefaultConfiguration spoolConf
                = new DefaultConfiguration("repository", "generated:RemoteDelivery.java");
            spoolConf.setAttribute("destinationURL", outgoingPath);
            spoolConf.setAttribute("type", "SPOOL");
            outgoing = (SpoolRepository) mailstore.select(spoolConf);
        } catch (ComponentException cnfe) {
            log("Failed to retrieve Store component:" + cnfe.getMessage());
        } catch (Exception e) {
            log("Failed to retrieve Store component:" + e.getMessage());
        }

        //Start up a number of threads
        try {
            deliveryThreadCount = Integer.parseInt(getInitParameter("deliveryThreads"));
        } catch (Exception e) {
        }
        for (int i = 0; i < deliveryThreadCount; i++) {
            Thread t = new Thread(this, "Remote delivery thread (" + i + ")");
            t.start();
            deliveryThreads.add(t);
        }
    }

    /**
     * We can assume that the recipients of this message are all going to the same
     * mail server.  We will now rely on the DNS server to do DNS MX record lookup
     * and try to deliver to the multiple mail servers.  If it fails, it should
     * throw an exception.
     *
     * Creation date: (2/24/00 11:25:00 PM)
     * @param Mail org.apache.mailet.Mail
     * @param Session javax.mail.Session
     * @return boolean Whether the delivery was successful and the message can be deleted
     */
    private boolean deliver(MailImpl mail, Session session) {
        try {
            log("attempting to deliver " + mail.getName());
            MimeMessage message = mail.getMessage();

            //Create an array of the recipients as InternetAddress objects
            Collection recipients = mail.getRecipients();
            InternetAddress addr[] = new InternetAddress[recipients.size()];
            int j = 0;
            for (Iterator i = recipients.iterator(); i.hasNext(); j++) {
                MailAddress rcpt = (MailAddress)i.next();
                addr[j] = rcpt.toInternetAddress();
            }

            //Figure out which servers to try to send to.  This collection
            //  will hold all the possible target servers
            Collection targetServers = null;
            if (gatewayServer == null) {
                MailAddress rcpt = (MailAddress) recipients.iterator().next();
                String host = rcpt.getHost();

                //Lookup the possible targets
                targetServers = getMailetContext().getMailServers(host);
                if (targetServers.size() == 0) {
                    log("No mail server found for: " + host);
                    return failMessage(mail, new MessagingException("No route found to " + host), true);
                }
            } else {
                targetServers = new Vector();
                targetServers.add(gatewayServer);
            }

            MessagingException lastError = null;

            if (addr.length > 0) {
                Iterator i = targetServers.iterator();
                while ( i.hasNext()) {
                    try {
                        String outgoingmailserver = i.next().toString ();
                        log("attempting delivery of " + mail.getName() + " to host " + outgoingmailserver + " to " + Arrays.asList(addr));
                        URLName urlname = new URLName("smtp://" + outgoingmailserver);

                        Properties props = session.getProperties();
                        //This was an older version of JavaMail
                        if (mail.getSender() == null) {
                            props.put("mail.smtp.user", "<>");
                            props.put("mail.smtp.from", "<>");
                        } else {
                            props.put("mail.smtp.user", mail.getSender().toString());
                            props.put("mail.smtp.from", mail.getSender().toString());
                        }

                        //Many of these properties are only in later JavaMail versions
                        //"mail.smtp.ehlo"  //default true
                        //"mail.smtp.auth"  //default false
                        //"mail.smtp.dsn.ret"  //default to nothing... appended as RET= after MAIL FROM line.
                        //"mail.smtp.dsn.notify" //default to nothing...appended as NOTIFY= after RCPT TO line.
                        //"mail.smtp.localhost" //local server name, InetAddress.getLocalHost().getHostName();

                        Transport transport = session.getTransport(urlname);
                        transport.connect();
                        transport.sendMessage(message, addr);
                        transport.close();
                        log("mail (" + mail.getName() + ") sent successfully to " + outgoingmailserver);
                        return true;
                    } catch (SendFailedException sfe) {
                        throw sfe;
                    } catch (MessagingException me) {
                        log("Exception delivering message (" + mail.getName() + ") - " + me.getMessage());
                        if (me.getNextException() != null && me.getNextException() instanceof java.io.IOException) {
                            //This is more than likely a temporary failure
                            lastError = me;
                            continue;
                        } else {
                            return failMessage(mail, me, true);
                        }
                    }
                } // end while
                //If we encountered an exception while looping through, send the last exception we got
                if (lastError != null) {
                    throw lastError;
                }
            } else {
                log("no recipients specified... not sure how this could have happened.");
            }
        } catch (SendFailedException sfe) {
            //Would like to log all the types of email addresses
            if (sfe.getValidSentAddresses() != null) {
                Address[] validSent = sfe.getValidSentAddresses();
                Collection recipients = mail.getRecipients();
                //Remove these addresses for the recipients
                for (int i = 0; i < validSent.length; i++) {
                    try {
                        MailAddress addr = new MailAddress(validSent[i].toString());
                        recipients.remove(addr);
                    } catch (ParseException pe) {
                        //ignore once debugging done
                        pe.printStackTrace();
                    }
                }
            }
            return failMessage(mail, sfe, true);
        } catch (MessagingException ex) {
            //We should do a better job checking this... if the failure is a general
            //connect exception, this is less descriptive than more specific SMTP command
            //failure... have to lookup and see what are the various Exception
            //possibilities

            //Unable to deliver message after numerous tries... fail accordingly
            return failMessage(mail, ex, false);
        }
        return true;
    }

    /**
     * Insert the method's description here.
     * Creation date: (2/25/00 1:14:18 AM)
     * @param mail org.apache.mailet.MailImpl
     * @param exception java.lang.Exception
     * @param boolean permanent
     * @return boolean Whether the message failed fully and can be deleted
     */
    private boolean failMessage(MailImpl mail, MessagingException ex, boolean permanent) {
        StringWriter sout = new StringWriter();
        PrintWriter pout = new PrintWriter(sout, true);
        if (permanent) {
            pout.print("Permanent");
        } else {
            pout.print("Temporary");
        }
        pout.print(" exception delivering mail (" + mail.getName() + ": ");
        ex.printStackTrace(pout);
        log(sout.toString());
        if (!permanent) {
            if (!mail.getState().equals(Mail.ERROR)) {
                mail.setState(Mail.ERROR);
                mail.setErrorMessage("0");
                mail.setLastUpdated(new Date());
            }
            int retries = Integer.parseInt(mail.getErrorMessage());
            if (retries < maxRetries) {
                log("Storing message " + mail.getName() + " into outgoing after " + retries + " retries");
                ++retries;
                mail.setErrorMessage(retries + "");
                mail.setLastUpdated(new Date());
                return false;
            }
        }
        bounce(mail, ex);
        return true;
    }

    private void bounce(MailImpl mail, MessagingException ex) {
        StringWriter sout = new StringWriter();
        PrintWriter pout = new PrintWriter(sout, true);
        String machine ="[unknown]";
        try{
            InetAddress me = InetAddress.getLocalHost();
            machine = me.getHostName();
        }catch(Exception e){
            machine = "[address unknown]";
        }
        pout.println("Hi. This is the James mail server at " + machine + ".");
        pout.println("I'm afraid I wasn't able to deliver your message to the following addresses.");
        pout.println("This is a permanent error; I've given up. Sorry it didn't work out.");
        pout.println();
        for (Iterator i = mail.getRecipients().iterator(); i.hasNext(); ) {
            pout.println(i.next());
        }
        pout.println(ex.getMessage().trim());
        pout.println();
        pout.println("The original message is attached.");

        log("Sending failure message " + mail.getName());
        try {
            getMailetContext().bounce(mail, sout.toString());
        } catch (MessagingException me) {
            log("encountered unexpected messaging exception while bouncing message: " + me.getMessage());
        }
    }

    public String getMailetInfo() {
        return "RemoteDelivery Mailet";
    }

    /**
     * For this message, we take the list of recipients, organize these into distinct
     * servers, and duplicate the message for each of these servers, and then call
     * the deliver (messagecontainer) method for each server-specific
     * messagecontainer ... that will handle storing it in the outgoing queue if needed.
     *
     * @param mail org.apache.mailet.Mail
     * @return org.apache.mailet.MessageContainer
     */
    public void service(Mail genericmail) throws AddressException {
        MailImpl mail = (MailImpl)genericmail;

        //Do I want to give the internal key, or the message's Message ID
        log("Remotely delivering mail " + mail.getName());
        Collection recipients = mail.getRecipients();

        //Must first organize the recipients into distinct servers (name made case insensitive)
        Hashtable targets = new Hashtable();
        for (Iterator i = recipients.iterator(); i.hasNext();) {
            MailAddress target = (MailAddress)i.next();
            String targetServer = target.getHost().toLowerCase();
            Collection temp = (Collection)targets.get(targetServer);
            if (temp == null) {
                temp = new Vector();
                targets.put(targetServer, temp);
            }
            temp.add(target);
        }

        //We have the recipients organized into distinct servers... put them into the
        //delivery store organized like this... this is ultra inefficient I think...

        //store the new message containers, organized by server, in the outgoing mail repository
        String name = mail.getName();
        for (Iterator i = targets.keySet().iterator(); i.hasNext(); ) {
            String host = (String) i.next();
            Collection rec = (Collection)targets.get(host);
            log("sending mail to " + rec + " on " + host);
            mail.setRecipients(rec);
            mail.setName(name + "-to-" + host);
            outgoing.store(mail);
            //Set it to try to deliver (in a separate thread) immediately (triggered by storage)
        }
        mail.setState(Mail.GHOST);
    }

    public void destroy() {
        //Wake up all threads from waiting for an accept
        notifyAll();
        for (Iterator i = deliveryThreads.iterator(); i.hasNext(); ) {
            Thread t = (Thread)i.next();
            t.interrupt();
        }
    }

    /**
     * Handles checking the outgoing spool for new mail and delivering them if
     * there are any
     */
    public void run() {
        //Checks the pool and delivers a mail message
        Properties props = new Properties();
        //Not needed for production environment
        props.put("mail.debug", "false");
        //Prevents problems encountered with 250 OK Messages
        props.put("mail.smtp.ehlo", "false");
        //Sets timeout on going connections
        props.put("mail.smtp.timeout", smtpTimeout + "");

        //If there's a gateway port, we can just set it here
        if (gatewayPort != null) {
            props.put("mail.smtp.port", gatewayPort);
        }
        Session session = Session.getInstance(props, null);
        while (!Thread.currentThread().interrupted()) {
            try {
                String key = outgoing.accept(delayTime);
                log(Thread.currentThread().getName() + " will process mail " + key);
                MailImpl mail = outgoing.retrieve(key);
                if (deliver(mail, session)) {
                    //Message was successfully delivered/fully failed... delete it
                    outgoing.remove(key);
                } else {
                    //Something happened that will delay delivery.  Store any updates
                    outgoing.store(mail);
                }
                //Clear the object handle to make sure it recycles this object.
                mail = null;
            } catch (Exception e) {
                log("Exception caught in RemoteDelivery.run(): " + e);
            }
        }
    }
}
