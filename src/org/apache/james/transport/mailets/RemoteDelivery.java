/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import java.io.*;
import java.util.*;
import java.net.*;

import org.apache.java.lang.*;
import org.apache.james.*;
import org.apache.mail.*;
import org.apache.avalon.interfaces.*;
import org.apache.james.transport.*;

import javax.mail.URLName;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.*;


/**
 * Receive  a MessageContainer from JamesSpoolManager and takes care of delivery
 * the message to remote hosts. If for some reason mail can't be delivered
 * store it in the "delayed" Repository and set an Alarm. After "delayTime" the
 * Alarm will wake the servlet that will try to send it again. After "maxRetries"
 * the mail will be considered underiverable and will be returned to sender.
 *
 * Note: Many FIXME on the air.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class RemoteDelivery extends AbstractMailet implements TimeServer.Bell {

    private MailRepository delayed;
    private TimeServer timeServer;
    private Logger logger;
    private long delayTime;
    private int maxRetries;
    private MailServer mailServer;

    private InternetAddress postmaster;
    private SmartTransport transport;

    public void init () throws Exception {
        MailetContext context = getContext();
        ComponentManager comp = context.getComponentManager();
        Configuration conf = context.getConfiguration();
        delayTime = conf.getConfiguration("delayTime", "21600000").getValueAsLong(); // default is 6*60*60*1000 mills
        maxRetries = conf.getConfiguration("maxRetries", "5").getValueAsInt(); // default is 5 retries
        timeServer = (TimeServer) comp.getComponent(Interfaces.TIME_SERVER);
        String delayedRepository = conf.getConfiguration("repository", "file://../var/mail/delayed").getValue();
        Store store = (Store) comp.getComponent(Interfaces.STORE);
        delayed = (MailRepository) store.getPrivateRepository(delayedRepository, MailRepository.MAIL, Store.ASYNCHRONOUS);
        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        StringBuffer servers = new StringBuffer ();
        for (Enumeration e = conf.getConfigurations("DNSservers.server") ; e.hasMoreElements(); ) {
            Configuration c = (Configuration)e.nextElement ();
            servers.append (c.getValue () + " ");
        }
        boolean authoritative = conf.getConfiguration("authoritative", "false").getValueAsBoolean();
        transport = new SmartTransport (servers.toString(), authoritative);
        postmaster = new InternetAddress((String) getContext().get(Resources.POSTMASTER));
        mailServer = (MailServer) comp.getComponent(Interfaces.MAIL_SERVER);

        int i = 0;
        for (Enumeration e = delayed.list(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            timeServer.setAlarm(key, this, ++i * 10000);
        }
    }

    /**
     * We can assume that the recipients of this message are all going to the same
     * mail server.  We will now rely on SmartTransport to do DNS MX record lookup
     * and try to deliver to the multiple mail servers.  If it fails, it should
     * throw an exception.
     *
     * Creation date: (2/24/00 11:25:00 PM)
     * @param Mail org.apache.mail.Mail
     */
    private void deliver(Mail mail) {
        try {
            MimeMessage message = mail.getMessage ();
            Vector recipients = mail.getRecipients ();
            String rec = (String) recipients.elementAt(0);
            String host = rec.substring(rec.indexOf('@') + 1);
            InternetAddress addr[] = new InternetAddress[recipients.size()];
            int j = 0;
            for (Enumeration e = recipients.elements(); e.hasMoreElements(); j++) {
                addr[j] = new InternetAddress((String) e.nextElement());
            }

            transport.sendMessage (message, addr);
            logger.log("Mail sent", "Mailets", Logger.INFO);
            //We've succeeded somehow!!!  Cheers!
            return;
        } catch (Exception ex) {
            //We should do a better job checking this... if the failure is a general
            //connect exception, this is less descriptive than more specific SMTP command
            //failure... have to lookup and see what are the various Exception
            //possibilities

            //Unable to deliver message after numerous tries... fail accordingly
//          ex.printStackTrace();
            failMessage (mail, "Delivery failure: " + ex.toString ());
        }
    }

    public void destroy() {
    }
/**
 * Insert the method's description here.
 * Creation date: (2/25/00 1:14:18 AM)
 * @param mail org.apache.mail.Mail
 * @param reason java.lang.String
 */
private void failMessage(Mail mail, String reason) {
    logger.log("Exception delivering mail: " + reason, "Mailets", Logger.INFO);
    if (!mail.getState().equals(Mail.ERROR)) {
        mail.setState(Mail.ERROR);
        mail.setErrorMessage("1");
    }
    int retries = Integer.parseInt(mail.getErrorMessage());
    if (retries > maxRetries) {
        logger.log("Sending back message " + mail.getName () + " after " + retries + " retries", "Mailets", Logger.INFO);
        //logger.log("Sending back message " + mail.getMessage ().getMessageID () + " after " + retries + " retries");
        try {
            //FIXME: need much better logging of why this message failed, including the
            //original message as an attachment.  q.q.v. old james stuff.
            MimeMessage reply = (MimeMessage) (mail.getMessage()).reply(false);
            reply.setSubject("Unable to deliver this message to recipients: " + reason);
            Vector recipients = new Vector ();
            recipients.addElement (mail.getSender ());
            InternetAddress addr[] = {new InternetAddress(mail.getSender())};
            reply.setRecipients(Message.RecipientType.TO, addr);
            reply.setFrom(postmaster);

            mailServer.sendMail (postmaster.toString (), recipients, reply);
        } catch (Exception ignore) {
            // FIXME: cannot destroy mails... what should we do here ?
            logger.log("Unable to reply. Destroying message", "Mailets", Logger.INFO);
            //This should go to straight to the postmaster... but I'm sleepy
        }
    } else {
        mail.setName(mail.getName() + retries);
        logger.log("Storing message " + mail.getName () + " into delayed after " + retries + " retries", "Mailets", Logger.INFO);
        mail.setErrorMessage("" + ++retries);
        delayed.store(mail);
        timeServer.setAlarm(mail.getName (), this, delayTime);
    }
}
    public String getServletInfo() {
        return "RemoteDelivery Mail Servlet";
    }
    /**
     * For this message, we take the list of recipients, organize these into distinct
     * servers, and duplicate the message for each of these servers, and then call
     * the deliver (messagecontainer) method for each server-specific
     * messagecontainer ... that will handle storing it in the delayed queue if needed.
     *
     * @param mail org.apache.mail.Mail
     * @return org.apache.mail.MessageContainer
     */
    public void service(Mail mail) {
        //Do I want to give the internal key, or the message's Message ID
        logger.log("Remotly delivering mail " + mail.getName(), "Mailets", Logger.INFO);
        Vector recipients = mail.getRecipients();

        //Must first organize the recipients into distinct servers (name made case insensitive)
        Hashtable targets = new Hashtable ();
        for (Enumeration e = recipients.elements(); e.hasMoreElements ();) {
            String target = (String)e.nextElement ();
            String targetServer = target.substring (target.indexOf ("@") + 1).toLowerCase ();
            Vector temp = (Vector)targets.get(targetServer);
            if (temp == null) {
                temp = new Vector ();
                targets.put (targetServer, temp);
            }
            temp.addElement (target);
        }

        //We have the recipients organized into distinct servers... put them into the
        //delivery store organized like this... this is ultra inefficient I think...

        //remove the original message from the container... I guess I would
        //just return null so the primary James stream stops consider this message
        // ??????? this really doesn't look right... it should work, but ugghgghghghhh...
        //delayed.remove (mail.getMessageId ());

        //store the new message containers - organized by server
        String name = mail.getName();
        for (Enumeration e = targets.keys(); e.hasMoreElements(); ) {
            String host = (String) e.nextElement();
            Vector rec = (Vector) targets.get(host);
            logger.log("Sending mail to " + rec + " on " + host, "Mailets", Logger.INFO);
            mail.setRecipients(rec);
            mail.setName(name + "-to-" + host);
            deliver (mail);
        }
        mail.setState(mail.GHOST);
    }

    public void wake(String name, String memo) {
        Mail mail = delayed.retrieve(name);
        deliver(mail);
        delayed.remove(name);
    }
}
