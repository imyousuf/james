package org.apache.james.transport.mailets;

/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

import java.io.*;
import java.util.*;
import java.net.*;

import org.apache.avalon.*;
import org.apache.james.*;
import org.apache.james.core.*;
import org.apache.james.transport.*;
import org.apache.mailet.*;
import org.apache.avalon.blocks.*;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.*;


/**
 * Receive  a MessageContainer from JamesSpoolManager and takes care of delivery
 * the message to remote hosts. If for some reason mail can't be delivered
 * store it in the "delayed" Repository and set an Alarm. After "delayTime" the
 * Alarm will wake the servlet that will try to send it again. After "maxRetries"
 * the mail will be considered underiverable and will be returned to sender.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 */
public class RemoteDelivery extends GenericMailet implements TimeServer.Bell {

    private MailRepository delayed;
    private TimeServer timeServer;
    private long delayTime = 21600000; // default is 6*60*60*1000 mills
    private int maxRetries = 5; // default number of retries
    private MailServer mailServer;

    public void init() throws MailetException {
        try {
            delayTime = Long.parseLong(getInitParameter("delayTime"));
        } catch (Exception e) {
        }
        try {
            maxRetries = Integer.parseInt(getInitParameter("maxRetries"));
        } catch (Exception e) {
        }
        ComponentManager comp = (ComponentManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        timeServer = (TimeServer) comp.getComponent(Interfaces.TIME_SERVER);

        // Instanziate the a MailRepository for delayed mails
        Store store = (Store) comp.getComponent(Interfaces.STORE);
        String delayedPath = getInitParameter("delayed");
        if (delayedPath == null) {
            delayedPath = "../var/mail/delayed";
        }
        delayed = (MailRepository) store.getPrivateRepository(delayedPath, MailRepository.MAIL, Store.ASYNCHRONOUS);

        int i = 0;
        for (Enumeration e = delayed.list(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            timeServer.setAlarm(key, this, ++i * 10000);
            log("delayed message " + key + " set for delivery in " + (i * 10) + " seconds");
        }
    }

    /**
     * We can assume that the recipients of this message are all going to the same
     * mail server.  We will now rely on SmartTransport to do DNS MX record lookup
     * and try to deliver to the multiple mail servers.  If it fails, it should
     * throw an exception.
     *
     * Creation date: (2/24/00 11:25:00 PM)
     * @param Mail org.apache.mailet.Mail
     */
    private void deliver(MailImpl mail) {
        try {
            MimeMessage message = mail.getMessage();
            Collection recipients = mail.getRecipients();
            MailAddress rec = (MailAddress) recipients.iterator().next();
            String host = rec.getHost();
            InternetAddress addr[] = new InternetAddress[recipients.size()];
            int j = 0;
            for (Iterator i = recipients.iterator(); i.hasNext(); j++) {
                addr[j] = new InternetAddress(i.next().toString());
            }

            if (addr.length > 0) {
                //Lookup the possible targets
                for (Iterator i = getMailetContext().getMailServers(host).iterator(); i.hasNext();) {
                    try {
                        String outgoingmailserver = i.next().toString ();
                        URLName urlname = new URLName("smtp://" + outgoingmailserver);

                        Transport transport = Session.getDefaultInstance(System.getProperties(), null).getTransport(urlname);

                        transport.connect ();
                        transport.sendMessage(message, addr);
                        transport.close ();
                        return;
                    } catch (MessagingException me) {
                        if (!i.hasNext())
                            throw me;
                    }
                }
                throw new MessagingException("No route found to " + host);
            }

            log("Mail sent");
            //We've succeeded somehow!!!  Cheers!
            return;
        } catch (Exception ex) {
            //We should do a better job checking this... if the failure is a general
            //connect exception, this is less descriptive than more specific SMTP command
            //failure... have to lookup and see what are the various Exception
            //possibilities

            //Unable to deliver message after numerous tries... fail accordingly
            ex.printStackTrace();
            failMessage (mail, "Delivery failure: " + ex.toString ());
        }
    }

    /**
     * Insert the method's description here.
     * Creation date: (2/25/00 1:14:18 AM)
     * @param mail org.apache.mailet.Mail
     * @param reason java.lang.String
     */
    private void failMessage(MailImpl mail, String reason) {
        log("Exception delivering mail: " + reason);
        if (!mail.getState().equals(Mail.ERROR)) {
            mail.setState(Mail.ERROR);
            mail.setErrorMessage("1");
        }
        int retries = Integer.parseInt(mail.getErrorMessage());
        if (retries > maxRetries) {
            log("Sending back message " + mail.getName () + " after " + retries + " retries");
            //log("Sending back message " + mail.getMessage ().getMessageID () + " after " + retries + " retries");
            try {
                //FIXME: need much better logging of why this message failed, including the
                //original message as an attachment.  q.q.v. old james stuff.
                MimeMessage reply = (MimeMessage) (mail.getMessage()).reply(false);
                reply.setSubject("Unable to deliver this message to recipients: " + reason);
                Collection recipients = new Vector ();
                recipients.add(mail.getSender().toString());
                InternetAddress addr[] = {new InternetAddress(mail.getSender().toString())};
                reply.setRecipients(javax.mail.Message.RecipientType.TO, addr);
                reply.setFrom(getMailetContext().getPostmaster().toInternetAddress());

                mailServer.sendMail(getMailetContext().getPostmaster(), recipients, reply);
            } catch (Exception ignore) {
                // FIXME: cannot destroy mails... what should we do here ?
                log("Unable to reply. Destroying message");
                //This should go to straight to the postmaster... but I'm sleepy
            }
        } else {
            mail.setName(mail.getName() + retries);
            log("Storing message " + mail.getName() + " into delayed after " + retries + " retries");
            ++retries;
            mail.setErrorMessage(retries + "");
            delayed.store(mail);
            timeServer.setAlarm(mail.getName(), this, delayTime);
        }
    }

    public String getMailetInfo() {
        return "RemoteDelivery Mailet";
    }

    /**
     * For this message, we take the list of recipients, organize these into distinct
     * servers, and duplicate the message for each of these servers, and then call
     * the deliver (messagecontainer) method for each server-specific
     * messagecontainer ... that will handle storing it in the delayed queue if needed.
     *
     * @param mail org.apache.mailet.Mail
     * @return org.apache.mailet.MessageContainer
     */
    public void service(Mail genericmail) throws AddressException {
        MailImpl mail = (MailImpl)genericmail;

        //Do I want to give the internal key, or the message's Message ID
        log("Remotly delivering mail " + mail.getName());
        Collection recipients = mail.getRecipients();

        //Must first organize the recipients into distinct servers (name made case insensitive)
        Hashtable targets = new Hashtable();
        for (Iterator i = recipients.iterator(); i.hasNext();) {
            MailAddress target = (MailAddress)i.next();
            String targetServer = target.getHost().toLowerCase();
            Collection temp = (Collection)targets.get(targetServer);
            if (temp == null) {
                temp = new Vector();
                targets.put (targetServer, temp);
            }
            temp.add(target);
        }

        //We have the recipients organized into distinct servers... put them into the
        //delivery store organized like this... this is ultra inefficient I think...

        //remove the original message from the container... I guess I would
        //just return null so the primary James stream stops consider this message
        // ??????? this really doesn't look right... it should work, but ugghgghghghhh...
        //delayed.remove (mail.getMessageId ());

        //store the new message containers - organized by server
        String name = mail.getName();
        for (Iterator i = targets.keySet().iterator(); i.hasNext(); ) {
            String host = (String) i.next();
            Collection rec = (Collection) targets.get(host);
            log("Sending mail to " + rec + " on " + host);
            mail.setRecipients(rec);
            mail.setName(name + "-to-" + host);
            deliver(mail);
        }
        mail.setState(Mail.GHOST);
    }

    public void wake(String name, String memo) {
        MailImpl mail = delayed.retrieve(name);
        deliver(mail);
        delayed.remove(name);
    }
}
