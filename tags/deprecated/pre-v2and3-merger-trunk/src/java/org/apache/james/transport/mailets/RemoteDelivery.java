/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.transport.mailets;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.ArrayList;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContextConstants;
import org.apache.mailet.SpoolRepository;

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
 * @version CVS $Revision: 1.52 $ $Date: 2004/01/30 02:22:12 $
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
    private boolean sendPartial = false; // If false then ANY address errors will cause the transmission to fail
    private int connectionTimeout = 60000;  // The amount of time JavaMail will wait before giving up on a socket connect()
    private int deliveryThreadCount = 1; // default number of delivery threads
    private Collection gatewayServer = null; // the server(s) to send all email to
    private String gatewayPort = null;  // the default port of gateway server(s)
    private String bindAddress = null; // JavaMail delivery socket binds to this local address. If null the JavaMail default will be used.
    private boolean isBindUsed = false; // true, if the bind configuration
                                        // parameter is supplied, RemoteDeliverySocketFactory
                                        // will be used in this case
    private Collection deliveryThreads = new Vector();

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

        try {
            if (getInitParameter("connectiontimeout") != null) {
                connectionTimeout = Integer.parseInt(getInitParameter("connectiontimeout"));
            }
        } catch (Exception e) {
            log("Invalid timeout setting: " + getInitParameter("timeout"));
        }
        sendPartial = (getInitParameter("sendpartial") == null) ? false : new Boolean(getInitParameter("sendpartial")).booleanValue();

        String gateway = getInitParameter("gateway");
        gatewayPort = getInitParameter("gatewayPort");

        if (gateway != null) {
            gatewayServer = new ArrayList();
            StringTokenizer st = new StringTokenizer(gateway, ",") ;
            while (st.hasMoreTokens()) {
                String server = st.nextToken().trim() ;
                if (server.indexOf(':') < 0 && gatewayPort != null) {
                    server += ":";
                    server += gatewayPort;
                }

                if (isDebug) log("Adding SMTP gateway: " + server) ;
                gatewayServer.add(server);
            }
        }

        outgoing = getMailetContext().getMailSpool(getInitParameter("outgoing"));

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

        bindAddress = getInitParameter("bind");
        isBindUsed = bindAddress != null;
        try {
            if (isBindUsed) RemoteDeliverySocketFactory.setBindAdress(bindAddress);
        } catch (UnknownHostException e) {
            log("Invalid bind setting (" + bindAddress + "): " + e.toString());
        }
    }

    /**
     * We can assume that the recipients of this message are all going to the same
     * mail server.  We will now rely on the DNS server to do DNS MX record lookup
     * and try to deliver to the multiple mail servers.  If it fails, it should
     * throw an exception.
     *
     * Creation date: (2/24/00 11:25:00 PM)
     * @param mail org.apache.mailet.Mail
     * @param session javax.mail.Session
     * @return boolean Whether the delivery was successful and the message can be deleted
     */
    private boolean deliver(Mail mail, Session session) {
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

            if (addr.length <= 0) {
                log("No recipients specified... not sure how this could have happened.");
                return true;
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
                targetServers = gatewayServer;
            }

            MessagingException lastError = null;

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
                    if (mail.getSender() == null) {
                        props.put("mail.smtp.from", "<>");
                    } else {
                        String sender = mail.getSender().toString();
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
                            // to connect to the next SMTP server associated with this
                            // MX record.  Just log the exception.  We'll worry about
                            // failing the message at the end of the loop.
                            log(me.getMessage());
                            continue;
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
                } catch (SendFailedException sfe) {
                    if (sfe.getValidSentAddresses() == null
                          || sfe.getValidSentAddresses().length < 1) {
                        if (isDebug) log("Send failed, continuing with any other servers");
                        lastError = sfe;
                        continue;
                    } else {
                        // If any mail was sent then the outgoing
                        // server config must be ok, therefore rethrow
                        throw sfe;
                    }
                } catch (MessagingException me) {
                    //MessagingException are horribly difficult to figure out what actually happened.
                    StringBuffer exceptionBuffer =
                        new StringBuffer(256)
                        .append("Exception delivering message (")
                        .append(mail.getName())
                        .append(") - ")
                        .append(me.getMessage());
                    log(exceptionBuffer.toString());
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
            //If we encountered an exception while looping through,
            //throw the last MessagingException we caught.  We only
            //do this if we were unable to send the message to any
            //server.  If sending eventually succeeded, we exit
            //deliver() though the return at the end of the try
            //block.
            if (lastError != null) {
                throw lastError;
            }
        } catch (SendFailedException sfe) {
            boolean deleteMessage = false;
            Collection recipients = mail.getRecipients();

            //Would like to log all the types of email addresses
            if (isDebug) log("Recipients: " + recipients);

            /*
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
            */

            /*
             * The rest of the recipients failed for one reason or
             * another.
             *
             * SendFailedException actually handles this for us.  For
             * example, if you send a message that has multiple invalid
             * addresses, you'll get a top-level SendFailedException
             * that that has the valid, valid-unsent, and invalid
             * address lists, with all of the server response messages
             * will be contained within the nested exceptions.  [Note:
             * the content of the nested exceptions is implementation
             * dependent.]
             *
             * sfe.getInvalidAddresses() should be considered permanent.
             * sfe.getValidUnsentAddresses() should be considered temporary.
             *
             * JavaMail v1.3 properly populates those collections based
             * upon the 4xx and 5xx response codes.
             *
             */

            if (sfe.getInvalidAddresses() != null) {
                Address[] address = sfe.getInvalidAddresses();
                if (address.length > 0) {
                    recipients.clear();
                    for (int i = 0; i < address.length; i++) {
                        try {
                            recipients.add(new MailAddress(address[i].toString()));
                        } catch (ParseException pe) {
                            // this should never happen ... we should have
                            // caught malformed addresses long before we
                            // got to this code.
                            log("Can't parse invalid address: " + pe.getMessage());
                        }
                    }
                    if (isDebug) log("Invalid recipients: " + recipients);
                    deleteMessage = failMessage(mail, sfe, true);
                }
            }

            if (sfe.getValidUnsentAddresses() != null) {
                Address[] address = sfe.getValidUnsentAddresses();
                if (address.length > 0) {
                    recipients.clear();
                    for (int i = 0; i < address.length; i++) {
                        try {
                            recipients.add(new MailAddress(address[i].toString()));
                        } catch (ParseException pe) {
                            // this should never happen ... we should have
                            // caught malformed addresses long before we
                            // got to this code.
                            log("Can't parse unsent address: " + pe.getMessage());
                        }
                    }
                    if (isDebug) log("Unsent recipients: " + recipients);
                    deleteMessage = failMessage(mail, sfe, false);
                }
            }

            return deleteMessage;
        } catch (MessagingException ex) {
            // We should do a better job checking this... if the failure is a general
            // connect exception, this is less descriptive than more specific SMTP command
            // failure... have to lookup and see what are the various Exception
            // possibilities

            // Unable to deliver message after numerous tries... fail accordingly

            // We check whether this is a 5xx error message, which
            // indicates a permanent failure (like account doesn't exist
            // or mailbox is full or domain is setup wrong).
            // We fail permanently if this was a 5xx error
            return failMessage(mail, ex, ('5' == ex.getMessage().charAt(0)));
        }

        /* If we get here, we've exhausted the loop of servers without
         * sending the message or throwing an exception.  One case
         * where this might happen is if we get a MessagingException on
         * each transport.connect(), e.g., if there is only one server
         * and we get a connect exception.
         */
        return failMessage(mail, new MessagingException("No mail server(s) available at this time."), false);
    }

    /**
     * Insert the method's description here.
     * Creation date: (2/25/00 1:14:18 AM)
     * @param mail org.apache.mailet.Mail
     * @param ex javax.mail.MessagingException
     * @param boolean permanent
     * @return boolean Whether the message failed fully and can be deleted
     */
    private boolean failMessage(Mail mail, MessagingException ex, boolean permanent) {
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

    private void bounce(Mail mail, MessagingException ex) {
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
     */
    public void service(Mail mail) throws AddressException {

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
                    temp = new ArrayList();
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
            // Store the mail unaltered for processing by the gateway server(s)
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
        long stop = System.currentTimeMillis() + 60000;
        while ((getMailetContext().getAttribute(MailetContextConstants.HELLO_NAME) == null)
               && stop > System.currentTimeMillis()) {
            try {
                Thread.sleep(1000);
            } catch (Exception ignored) {} // wait for James to finish initializing
        }

        //Checks the pool and delivers a mail message
        Properties props = new Properties();
        //Not needed for production environment
        props.put("mail.debug", "false");
        //Prevents problems encountered with 250 OK Messages
        props.put("mail.smtp.ehlo", "false");
        //Sets timeout on going connections
        props.put("mail.smtp.timeout", smtpTimeout + "");

        props.put("mail.smtp.connectiontimeout", connectionTimeout + "");
        props.put("mail.smtp.sendpartial",String.valueOf(sendPartial));

        //Set the hostname we'll use as this server
        if (getMailetContext().getAttribute(MailetContextConstants.HELLO_NAME) != null) {
            props.put("mail.smtp.localhost", (String) getMailetContext().getAttribute(MailetContextConstants.HELLO_NAME));
        }
        else {
            Collection servernames = (Collection) getMailetContext().getAttribute(MailetContextConstants.SERVER_NAMES);
            if ((servernames != null) && (servernames.size() > 0)) {
                props.put("mail.smtp.localhost", (String) servernames.iterator().next());
            }
        }

        if (isBindUsed) {
            // undocumented JavaMail 1.2 feature, smtp transport will use
            // our socket factory, which will also set the local address
            props.put("mail.smtp.socketFactory.class",
                      "org.apache.james.transport.mailets.RemoteDeliverySocketFactory");
            // Don't fallback to the standard socket factory on error, do throw an exception
            props.put("mail.smtp.socketFactory.fallback", "false");
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
                        Mail mail = outgoing.retrieve(key);
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
                } catch (Throwable e) {
                    if (!destroyed) log("Exception caught in RemoteDelivery.run()", e);
                }
            }
        } finally {
            // Restore the thread state to non-interrupted.
            Thread.currentThread().interrupted();
        }
    }
}
