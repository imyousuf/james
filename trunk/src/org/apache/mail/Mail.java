/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.mail;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Wrap a mail (viewed as Stream).
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class Mail implements Serializable, Cloneable {

    public final static String DEFAULT = "DEFAULT";
    public final static String ERROR = "ERROR";

    private String errorMessage;
    private String state;
    private MimeMessage message;
    private InputStream messageIn;
    private String sender;
    private Vector recipients;
    private String name;


    public Mail() {
    }

    public Mail(String name, String sender, Vector recipients) {
        this();
        this.name = name;
        this.sender = sender;
        this.recipients = recipients;
    }

    public Mail(String name, String sender, Vector recipients, InputStream msg)
    throws MessagingException {
        this(name, sender, recipients);
        this.setMessage(msg);
    }

    public Mail(String name, String sender, Vector recipients, MimeMessage message) {
        this(name, sender, recipients);
        this.setMessage(message);
    }
    
    public String getName() {
        return name;
    }
    
    public void setErrorMessage(String msg) {
        this.errorMessage = msg;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
    
    public void setRecipients(Vector recipients) {
        this.recipients = recipients;
    }

    public Vector getRecipients() {
        return recipients;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getSender() {
        return sender;
    }

    public void setMessage(MimeMessage message) {
        this.message = message;
        this.messageIn = null;
    }

    public void setMessage(InputStream in)
    throws MessagingException {
        this.messageIn = new BufferedInputStream(in);
        this.message = null;
    }

    public MimeMessage getMessage() 
    throws MessagingException {
        parse();
        return message;
    }
    
    public void parse() 
    throws MessagingException {
        if (messageIn != null) {
            message = new MimeMessage(Session.getDefaultInstance(System.getProperties(), null), messageIn);
            messageIn = null;
        }
    }
    
    public void writeMessageTo(OutputStream out)
    throws IOException, MessagingException {
        if (message != null) {
            message.writeTo(out);
        } else {
// FIXME!!! need a smarter way to read many times the same stream!!!            
            messageIn.mark(1024 * 16);
            for (int next; (next = messageIn.read()) != -1; out.write(next));
            messageIn.reset();
        }
    }

    public void clean() {
        message = null;
        messageIn = null;
    }
    
    public Mail duplicate() {
        try {
            return new Mail(name, sender, recipients, getMessage());
        } catch (MessagingException me) {
        }
        return (Mail) null;
    }


    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException
    {
        out.writeObject(sender);
        out.writeObject(recipients);
        out.writeObject(state);
        out.writeObject(errorMessage);
        out.writeObject(name);
        
    }

    private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        sender = (String) in.readObject();
        recipients = (Vector) in.readObject();
        state = (String) in.readObject();
        errorMessage = (String) in.readObject();
        name = (String) in.readObject();
    }
}

