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
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.*;
import org.apache.avalon.framework.component.ComponentException;
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
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 */
public class RemoteDelivery extends GenericMailet implements Runnable {

    private SpoolRepository outgoing;
    private long delayTime = 21600000; // default is 6*60*60*1000 millis (6 hours)
    private int maxRetries = 5; // default number of retries
    private int deliveryThreadCount = 1; // default number of delivery threads
    private Collection deliveryThreads = new Vector();
    private MailServer mailServer;

    public void init() throws MessagingException {
        try {
            delayTime = Long.parseLong(getInitParameter("delayTime"));
        } catch (Exception e) {
        }
        try {
            maxRetries = Integer.parseInt(getInitParameter("maxRetries"));
        } catch (Exception e) {
        }
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
            spoolConf.addAttribute("destinationURL", outgoingPath);
            spoolConf.addAttribute("type", "SPOOL");
            spoolConf.addAttribute("model", "SYNCHRONOUS");

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
     */
    private void deliver(MailImpl mail, Session session) {
        try {
            log("attempting to deliver " + mail.getName());
            MimeMessage message = mail.getMessage();
            Collection recipients = mail.getRecipients();
            MailAddress rcpt = (MailAddress) recipients.iterator().next();
            String host = rcpt.getHost();
            InternetAddress addr[] = new InternetAddress[recipients.size()];
            int j = 0;
            for (Iterator i = recipients.iterator(); i.hasNext(); j++) {
                rcpt = (MailAddress)i.next();
                addr[j] = rcpt.toInternetAddress();
            }
            Exception e = null;

            if (addr.length > 0) {
                //Lookup the possible targets
                Iterator i = getMailetContext().getMailServers(host).iterator();
                if (! i.hasNext()) {
                    log("No mail servers found for: " + host);
                }
                while ( i.hasNext()) {
                    try {
                        String outgoingmailserver = i.next().toString ();
                        log("attempting delivery of " + mail.getName() + " to host " + outgoingmailserver);
                        URLName urlname = new URLName("smtp://" + outgoingmailserver);

                        Properties props = session.getProperties();
                        //This was an older version of JavaMail
                        props.put("mail.smtp.user", mail.getSender().toString());
                        props.put("mail.smtp.from", mail.getSender().toString());
                        props.put("mail.debug", "false");

                        //Many of these properties are only in later JavaMail versions
                        //"mail.smtp.ehlo"  //default true
                        //"mail.smtp.auth"  //default false
                        //"mail.smtp.port"  //default 25
                        //"mail.smtp.dsn.ret"  //default to nothing... appended as RET= after MAIL FROM line.
                        //"mail.smtp.dsn.notify" //default to nothing...appended as NOTIFY= after RCPT TO line.
                        //"mail.smtp.localhost" //local server name, InetAddress.getLocalHost().getHostName();


                        Transport transport = session.getTransport(urlname);
                        transport.connect();
                        transport.sendMessage(message, addr);
                        transport.close();
                        log("mail (" + mail.getName() + ") sent successfully to " + outgoingmailserver);
                        return;
                    } catch (MessagingException me) {
                        log("Exception caught in RemoteDelivery.deliver() : " + me);
                        e = me;
                        /*
                          } catch (java.net.SocketException se) {
                          //Only remember this exception if we received no other exception
                          if (e == null) {
                          e = se;
                          }
                          } catch (java.net.UnknownHostException uhe) {
                          //Only remember this exception if we received no other exception
                          if (e == null) {
                          e = uhe;
                          }
                        */
                    }
                }// end while
                //If we encountered an exception while looping through, send the last exception we got
                if (e != null) {
                    throw e;
                }
                throw new MessagingException("No route found to " + host);
            } else {
                log("no recipients specified... not sure how this could have happened.");
            }
        } catch (Exception ex) {
            //We should do a better job checking this... if the failure is a general
            //connect exception, this is less descriptive than more specific SMTP command
            //failure... have to lookup and see what are the various Exception
            //possibilities

            //Unable to deliver message after numerous tries... fail accordingly
            failMessage(mail, "Delivery failure: " + ex.toString(), ex);
        }
    }

    /**
     * Insert the method's description here.
     * Creation date: (2/25/00 1:14:18 AM)
     * @param mail org.apache.mailet.Mail
     * @param reason java.lang.String
     */
    private void failMessage(MailImpl mail, String reason, Exception ex) {
        StringWriter sout = new StringWriter();
        PrintWriter pout = new PrintWriter(sout, true);
        ex.printStackTrace(pout);
        log("Exception delivering mail (" + mail.getName() + ": " + sout.toString());
        if (!mail.getState().equals(Mail.ERROR)) {
            mail.setState(Mail.ERROR);
            mail.setErrorMessage("0");
        }
        int retries = Integer.parseInt(mail.getErrorMessage());
        if (retries >= maxRetries) {
            log("Sending back message " + mail.getName() + " after max (" + retries + ") retries reached");
            try {
                getMailetContext().bounce(mail, reason);
            } catch (MessagingException me) {
                log("encountered unexpected messaging exception while bouncing message: " + me.getMessage());
            }
        } else {
            //Change the name (unique identifier) of this message... we want to save a new copy
            // of it, so change the unique idea for restoring
            mail.setName(mail.getName() + retries);
            log("Storing message " + mail.getName() + " into outgoing after " + retries + " retries");
            ++retries;
            mail.setErrorMessage(retries + "");
            outgoing.store(mail);
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
        Session session = Session.getInstance(props, null);
        while (!Thread.currentThread().interrupted()) {
            try {
                String key = outgoing.accept(delayTime);
                log(Thread.currentThread().getName() + " will process mail " + key);
                MailImpl mail = outgoing.retrieve(key);
                deliver(mail, session);
                outgoing.remove(key);
                mail = null;
            } catch (Exception e) {
                log("Exception caught in RemoteDelivery.run(): " + e);
            }
        }
    }
}
