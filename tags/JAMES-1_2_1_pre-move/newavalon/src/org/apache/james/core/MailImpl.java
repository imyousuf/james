/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.core;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.mailet.*;

/**
 * Wrap a MimeMessage adding routing informations (from SMTP) and same simple API.
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public class MailImpl implements Mail {
    //We hardcode the serialVersionUID so that from James 1.2 on,
    //  MailImpl will be deserializable (so your mail doesn't get lost)
    public static final long serialVersionUID = -4289663364703986260L;

    private String errorMessage;
    private String state;
    private MimeMessage message;
    private MailAddress sender;
    private Collection recipients;
    private String name;
    private String remoteHost = "localhost";
    private String remoteAddr = "127.0.0.1";
    private Date lastUpdated = new Date();

    public MailImpl() {
        setState(Mail.DEFAULT);
    }

    public MailImpl(String name, MailAddress sender, Collection recipients) {
        this();
        this.name = name;
        this.sender = sender;
        this.recipients = recipients;
    }

    public MailImpl(String name, MailAddress sender, Collection recipients, InputStream messageIn)
    throws MessagingException {
        this(name, sender, recipients);
        this.setMessage(messageIn);
    }

    public MailImpl(String name, MailAddress sender, Collection recipients, MimeMessage message) {
        this(name, sender, recipients);
        this.setMessage(message);
    }

    public void clean() {
        message = null;
    }

    public Mail duplicate() {
        try {
            return new MailImpl(name, sender, recipients, getMessage());
        } catch (MessagingException me) {
        }
        return (Mail) null;
    }

    public Mail duplicate(String newName) {
        try {
            return new MailImpl(newName, sender, recipients, getMessage());
        } catch (MessagingException me) {
        }
        return (Mail) null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public MimeMessage getMessage() throws MessagingException {
        return message;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Collection getRecipients() {
        return recipients;
    }

    public MailAddress getSender() {
        return sender;
    }

    public String getState() {
        return state;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    private void parse(InputStream messageIn) throws MessagingException {
        if (messageIn != null) {
            message = new EnhancedMimeMessage(Session.getDefaultInstance(System.getProperties(), null), messageIn);
        } else {
	    throw new MessagingException("Attempt to parse null input stream.");
	}
    }

    /**
     * <p>Return the size of the message including its headers.
     * MimeMessage.getSize() method only returns the size of the
     * message body.</p>
     *
     * <p>Note: this size is not guaranteed to be accurate - see Sun's
     * documentation of MimeMessage.getSize().</p>
     *
     * @return approximate size of full message including headers.
     *
     * @author Stuart Roebuck <stuart.roebuck@adolos.co.uk>
     */
    public int getSize() throws MessagingException {
        //SK: Should probably eventually store this as a locally
        //  maintained value (so we don't have to load and reparse
        //  messages each time).
        int size = message.getSize();
        Enumeration e = message.getAllHeaders();
        while (e.hasMoreElements()) {
            size += ((Header)e.nextElement()).toString().length();
         }
        return size;
     }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            sender = new MailAddress((String) in.readObject());
        } catch (ParseException pe) {
            throw new IOException("Error parsing sender address: " + pe.getMessage());
        }
        recipients = (Collection) in.readObject();
        state = (String) in.readObject();
        errorMessage = (String) in.readObject();
        name = (String) in.readObject();
        remoteHost = (String) in.readObject();
        remoteAddr = (String) in.readObject();
        lastUpdated = (Date) in.readObject();
    }

    public void setErrorMessage(String msg) {
        this.errorMessage = msg;
    }

    public void setMessage(InputStream in) throws MessagingException {
        this.message = new JamesMimeMessage(Session.getDefaultInstance(System.getProperties(), null), in);
    }

    public void setMessage(MimeMessage message) {
        this.message = message;
    }

    public void setRecipients(Collection recipients) {
        this.recipients = recipients;
    }

    public void setSender(MailAddress sender) {
        this.sender = sender;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void writeMessageTo(OutputStream out) throws IOException, MessagingException {
        if (message != null) {
            message.writeTo(out);
        } else {
	    throw new MessagingException("No message set for this MailImpl.");
	}
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        //System.err.println("saving object");
        lastUpdated = new Date();
        out.writeObject(sender.toString());
        out.writeObject(recipients);
        out.writeObject(state);
        out.writeObject(errorMessage);
        out.writeObject(name);
        out.writeObject(remoteHost);
        out.writeObject(remoteAddr);
        out.writeObject(lastUpdated);
    }

    public Mail bounce(String message) throws MessagingException {

        //This sends a message to the james component that is a bounce of the sent message
        MimeMessage original = getMessage();
        MimeMessage reply = (MimeMessage) original.reply(false);
        reply.setSubject("Re: " + original.getSubject());
        Collection recipients = new HashSet();
        recipients.add(getSender());
        InternetAddress addr[] = {new InternetAddress(getSender().toString())};
        reply.setRecipients(Message.RecipientType.TO, addr);
        reply.setFrom(new InternetAddress(getRecipients().iterator().next().toString()));
        reply.setText(message);
        reply.setHeader("Message-Id", "replyTo-" + getName());

        return new MailImpl("replyTo-" + getName(), new MailAddress(getRecipients().iterator().next().toString()), recipients, reply);
    }

    public void writeContentTo(OutputStream out, int lines)
           throws IOException, MessagingException {
        String line;
        BufferedReader br;
        if(message != null) {
            br = new BufferedReader(new InputStreamReader(message.getInputStream()));
            while(lines-- > 0) {
                if((line = br.readLine()) == null)  break;
                line += "\r\n";
                out.write(line.getBytes());
            }
        } else {
	    throw new MessagingException("No message set for this MailImpl.");
	}
    }
}
