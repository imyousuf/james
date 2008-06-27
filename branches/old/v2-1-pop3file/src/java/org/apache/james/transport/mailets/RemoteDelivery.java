/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.Constants;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailServer;
import org.apache.james.services.MailStore;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.*;
import java.util.*;

/**
 * Receives a MessageContainer from JamesSpoolManager and takes care of delivery
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
 * You really want to read the JavaMail documentation if you are
 * working in here, and you will want to view the list of JavaMail
 * attributes, which are documented here:
 * 
 * http://java.sun.com/products/javamail/1.3/docs/javadocs/com/sun/mail/smtp/package-summary.html
 *
 * as well as other places.
 * 
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 *
 * This is $Revision: 1.33 $
 */
public class RemoteDelivery extends GenericMailet implements Runnable {

    /**
     * Controls certain log messages
     */
    private boolean isDebug = false;

    private SpoolRepository outgoing; // The spool of outgoing mail
    private long delayTime = 21600000; // default is 6*60*60*1000 millis (6 hours)
    private int maxRetries = 5; // default number of retries
    private long smtpTimeout = 600000;  //default number of ms to timeout on smtp delivery
    private int deliveryThreadCount = 1; // default number of delivery threads
    private String gatewayServer = null; // the server to send all email to
    private String gatewayPort = null;  //the port of the gateway server to send all email to
    private Collection deliveryThreads = new Vector();
    private MailServer mailServer;
    private volatile boolean destroyed = false; //Flag that the run method will check and end itself if set to true

    /**
     * Initialize the mailet
     */
    public void init() throws MessagingException {
        isDebug = (getInitParameter("debug") == null) ? false : new Boolean(getInitParameter("debug")).booleanValue();
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
            StringBuffer nameBuffer =
                new StringBuffer(32)
                        .append("Remote delivery thread (")
                        .append(i)
                        .append(")");
            Thread t = new Thread(this, nameBuffer.toString());
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
            if (isDebug) {
                log("Attempting to deliver " + mail.getName());
            }
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
                    StringBuffer exceptionBuffer =
                        new StringBuffer(128)
                            .append("There are no DNS entries for the hostname ")
                            .append(host)
                            .append(".  I cannot determine where to send this message.");
                    return failMessage(mail, new MessagingException(exceptionBuffer.toString()), false);
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
                        String outgoingMailServer = i.next().toString ();
                        StringBuffer logMessageBuffer =
                            new StringBuffer(256)
                                    .append("Attempting delivery of ")
                                    .append(mail.getName())
                                    .append(" to host ")
                                    .append(outgoingMailServer)
                                    .append(" to addresses ")
                                    .append(Arrays.asList(addr));
                        log(logMessageBuffer.toString());
                        URLName urlname = new URLName("smtp://" + outgoingMailServer);

                        Properties props = session.getProperties();
                        //This was an older version of JavaMail
                        if (mail.getSender() == null) {
                            props.put("mail.smtp.user", "<>");
                            props.put("mail.smtp.from", "<>");
                        } else {
                            String sender = mail.getSender().toString();
                            props.put("mail.smtp.user", sender);
                            props.put("mail.smtp.from", sender);
                        }

                        //Many of these properties are only in later JavaMail versions
                        //"mail.smtp.ehlo"  //default true
                        //"mail.smtp.auth"  //default false
                        //"mail.smtp.dsn.ret"  //default to nothing... appended as RET= after MAIL FROM line.
                        //"mail.smtp.dsn.notify" //default to nothing...appended as NOTIFY= after RCPT TO line.

                        Transport transport = null;
                        try {
                            transport = session.getTransport(urlname);
                            try {
                                transport.connect();
                            } catch (MessagingException me) {
                                // Any error on connect should cause the mailet to attempt
                                // to connect to the next SMTP server associated with this MX record,
                                // assuming the number of retries hasn't been exceeded.
                                if (failMessage(mail, me, false)) {
                                    return true;
                                } else {
                                    continue;
                                }
                            }
                            transport.sendMessage(message, addr);
                        } finally {
                            if (transport != null) {
                                transport.close();
                                transport = null;
                            }
                        }
                        logMessageBuffer =
                            new StringBuffer(256)
                                    .append("Mail (")
                                    .append(mail.getName())
                                    .append(") sent successfully to ")
                                    .append(outgoingMailServer);
                        log(logMessageBuffer.toString());
                        return true;
                    } catch (MessagingException me) {
                        //MessagingException are horribly difficult to figure out what actually happened.
                        StringBuffer exceptionBuffer =
                            new StringBuffer(256)
                                    .append("Exception delivering message (")
                                    .append(mail.getName())
                                    .append(") - ")
                                    .append(me.getMessage());
                        log(exceptionBuffer.toString());
                        //Assume it is a permanent exception, or prove ourselves otherwise
                        boolean permanent = true;
                        if ((me.getNextException() != null) && 
                            (me.getNextException() instanceof java.io.IOException)) {
                            //This is more than likely a temporary failure

                            // If it's an IO exception with no nested exception, it's probably
                            // some socket or weird I/O related problem.
                            lastError = me;
                            continue;
                        }
                        // This was not a connection or I/O error particular to one
                        // SMTP server of an MX set.  Instead, it is almost certainly
                        // a protocol level error.  In this case we assume that this
                        // is an error we'd encounter with any of the SMTP servers
                        // associated with this MX record, and we pass the exception
                        // to the code in the outer block that determines its severity.
                        throw me;
                    }
                } // end while
                //If we encountered an exception while looping through, send the last exception we got
                if (lastError != null) {
                    throw lastError;
                }
            } else {
                log("No recipients specified... not sure how this could have happened.");
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
            // The rest of the recipients failed for one reason or another.
            // let failMessage handle it -- 5xx codes are permanent, others temporary.
            return failMessage(mail, sfe, (('5' == sfe.getMessage().charAt(0)) ? true : false));
        } catch (MessagingException ex) {
            // We should do a better job checking this... if the failure is a general
            // connect exception, this is less descriptive than more specific SMTP command
            // failure... have to lookup and see what are the various Exception
            // possibilities

            //Unable to deliver message after numerous tries... fail accordingly
            return failMessage(mail, ex, (('5' == ex.getMessage().charAt(0)) ? true : false));
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
        PrintWriter out = new PrintWriter(sout, true);
        if (permanent) {
            out.print("Permanent");
        } else {
            out.print("Temporary");
        }
        StringBuffer logBuffer =
            new StringBuffer(64)
                .append(" exception delivering mail (")
                .append(mail.getName())
                .append(": ");
        out.print(logBuffer.toString());
        ex.printStackTrace(out);
        log(sout.toString());
        if (!permanent) {
            if (!mail.getState().equals(Mail.ERROR)) {
                mail.setState(Mail.ERROR);
                mail.setErrorMessage("0");
                mail.setLastUpdated(new Date());
            }
            int retries = Integer.parseInt(mail.getErrorMessage());
            if (retries < maxRetries) {
                logBuffer =
                    new StringBuffer(128)
                            .append("Storing message ")
                            .append(mail.getName())
                            .append(" into outgoing after ")
                            .append(retries)
                            .append(" retries"); 
                log(logBuffer.toString());
                ++retries;
                mail.setErrorMessage(retries + "");
                mail.setLastUpdated(new Date());
                return false;
            } else {
                logBuffer =
                    new StringBuffer(128)
                            .append("Bouncing message ")
                            .append(mail.getName())
                            .append(" after ")
                            .append(retries)
                            .append(" retries"); 
                log(logBuffer.toString());
            }
        }
        bounce(mail, ex);
        return true;
    }

    private void bounce(MailImpl mail, MessagingException ex) {
        StringWriter sout = new StringWriter();
        PrintWriter out = new PrintWriter(sout, true);
        String machine = "[unknown]";
        try {
            InetAddress me = InetAddress.getLocalHost();
            machine = me.getHostName();
        } catch(Exception e){
            machine = "[address unknown]";
        }
        StringBuffer bounceBuffer =
            new StringBuffer(128)
                    .append("Hi. This is the James mail server at ")
                    .append(machine)
                    .append(".");
        out.println(bounceBuffer.toString());
        out.println("I'm afraid I wasn't able to deliver your message to the following addresses.");
        out.println("This is a permanent error; I've given up. Sorry it didn't work out.  Below");
        out.println("I include the list of recipients and the reason why I was unable to deliver");
        out.println("your message.");
        out.println();
        for (Iterator i = mail.getRecipients().iterator(); i.hasNext(); ) {
            out.println(i.next());
        }
        if (ex.getNextException() == null) {
            out.println(ex.getMessage().trim());
        } else {
            Exception ex1 = ex.getNextException();
            if (ex1 instanceof SendFailedException) {
                out.println("Remote mail server told me: " + ex1.getMessage().trim());
            } else if (ex1 instanceof UnknownHostException) {
                out.println("Unknown host: " + ex1.getMessage().trim());
                out.println("This could be a DNS server error, a typo, or a problem with the recipient's mail server.");
            } else if (ex1 instanceof ConnectException) {
                //Already formatted as "Connection timed out: connect"
                out.println(ex1.getMessage().trim());
            } else if (ex1 instanceof SocketException) {
                out.println("Socket exception: " + ex1.getMessage().trim());
            } else {
                out.println(ex1.getMessage().trim());
            }
        }
        out.println();
        out.println("The original message is attached.");

        log("Sending failure message " + mail.getName());
        try {
            getMailetContext().bounce(mail, sout.toString());
        } catch (MessagingException me) {
            log("Encountered unexpected messaging exception while bouncing message: " + me.getMessage());
        } catch (Exception e) {
            log("Encountered unexpected exception while bouncing message: " + e.getMessage());
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

        // Do I want to give the internal key, or the message's Message ID
        if (isDebug) {
            log("Remotely delivering mail " + mail.getName());
        }
        Collection recipients = mail.getRecipients();

        if (gatewayServer == null) {
            // Must first organize the recipients into distinct servers (name made case insensitive)
            Hashtable targets = new Hashtable();
            for (Iterator i = recipients.iterator(); i.hasNext();) {
                MailAddress target = (MailAddress)i.next();
                String targetServer = target.getHost().toLowerCase(Locale.US);
                Collection temp = (Collection)targets.get(targetServer);
                if (temp == null) {
                    temp = new Vector();
                    targets.put(targetServer, temp);
                }
                temp.add(target);
            }

            //We have the recipients organized into distinct servers... put them into the
            //delivery store organized like this... this is ultra inefficient I think...

            // Store the new message containers, organized by server, in the outgoing mail repository
            String name = mail.getName();
            for (Iterator i = targets.keySet().iterator(); i.hasNext(); ) {
                String host = (String) i.next();
                Collection rec = (Collection) targets.get(host);
                if (isDebug) {
                    StringBuffer logMessageBuffer =
                        new StringBuffer(128)
                                .append("Sending mail to ")
                                .append(rec)
                                .append(" on host ")
                                .append(host);
                    log(logMessageBuffer.toString());
                }
                mail.setRecipients(rec);
                StringBuffer nameBuffer =
                    new StringBuffer(128)
                            .append(name)
                            .append("-to-")
                            .append(host);
                mail.setName(nameBuffer.toString());
                outgoing.store(mail);
                //Set it to try to deliver (in a separate thread) immediately (triggered by storage)
            }
        } else {
            // Store the mail unaltered for processing by the gateway server
            if (isDebug) {
                StringBuffer logMessageBuffer =
                    new StringBuffer(128)
                        .append("Sending mail to ")
                        .append(mail.getRecipients())
                        .append(" via ")
                        .append(gatewayServer);
                log(logMessageBuffer.toString());
            }

             //Set it to try to deliver (in a separate thread) immediately (triggered by storage)
            outgoing.store(mail);
        }
        mail.setState(Mail.GHOST);
    }

    // Need to synchronize to get object monitor for notifyAll()
    public synchronized void destroy() {
        //Mark flag so threads from this mailet stop themselves
        destroyed = true;
        //Wake up all threads from waiting for an accept
        for (Iterator i = deliveryThreads.iterator(); i.hasNext(); ) {
            Thread t = (Thread)i.next();
            t.interrupt();
        }
        notifyAll();
    }

    /**
     * Handles checking the outgoing spool for new mail and delivering them if
     * there are any
     */
    public void run() {

        /* TODO: CHANGE ME!!! The problem is that we need to wait for James to
         * finish initializing.  We expect the HELLO_NAME to be put into
         * the MailetContext, but in the current configuration we get
         * started before the SMTP Server, which establishes the value.
         * Since there is no contractual guarantee that there will be a
         * HELLO_NAME value, we can't just wait for it.  As a temporary
         * measure, I'm inserting this philosophically unsatisfactory
         * fix.
         */
        try {
            Thread.sleep(5000);
        } catch (Exception ignored) {} // wait for James to finish initializing

        //Checks the pool and delivers a mail message
        Properties props = new Properties();
        //Not needed for production environment
        props.put("mail.debug", "false");
        //Prevents problems encountered with 250 OK Messages
        props.put("mail.smtp.ehlo", "false");
        //Sets timeout on going connections
        props.put("mail.smtp.timeout", smtpTimeout + "");
        //Set the hostname we'll use as this server
        if (getMailetContext().getAttribute(Constants.HELLO_NAME) != null) {
            props.put("mail.smtp.localhost", (String) getMailetContext().getAttribute(Constants.HELLO_NAME));
        }
        else {
            Collection servernames = (Collection) getMailetContext().getAttribute(Constants.SERVER_NAMES);
            if ((servernames != null) && (servernames.size() > 0)) {
                props.put("mail.smtp.localhost", (String) servernames.iterator().next());
            }
        }

        //If there's a gateway port, we can just set it here
        if (gatewayPort != null) {
            props.put("mail.smtp.port", gatewayPort);
        }
        Session session = Session.getInstance(props, null);
        try {
            while (!Thread.currentThread().interrupted() && !destroyed) {
                try {
                    String key = outgoing.accept(delayTime);
                    try {
                        if (isDebug) {
                            StringBuffer logMessageBuffer = 
                                new StringBuffer(128)
                                        .append(Thread.currentThread().getName())
                                        .append(" will process mail ")
                                        .append(key);
                            log(logMessageBuffer.toString());
                        }
                        MailImpl mail = outgoing.retrieve(key);
                        // Retrieve can return null if the mail is no longer on the outgoing spool.
                        // In this case we simply continue to the next key
                        if (mail == null) {
                            continue;
                        }
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
                        // Prevent unexpected exceptions from causing looping by removing
                        // message from outgoing.
                        outgoing.remove(key);
                        throw e;
                    }
                } catch (Exception e) {
                    log("Exception caught in RemoteDelivery.run(): " + e);
                }
            }
        } finally {
            // Restore the thread state to non-interrupted.
            Thread.currentThread().interrupted();
        }
    }
}
