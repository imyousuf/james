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
import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.configuration.*;

/**
 * Wrap a mail (viewed as Stream).
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class MessageContainer implements Serializable {

    public final static String GHOST = "GHOST";
    public final static String EMPTY = "EMPTY";
    public final static String DEFAULT = "DEFAULT";
    public final static String ERROR = "ERROR";

    private String errorMessage;
    private String messageId;
    private String state;
    private MimeMessage message;
    private String sender;
    private Vector recipients;

    public MessageContainer() {
        state = GHOST;
    }

    public MessageContainer(String sender, Vector recipients) {
        this();
        this.sender = sender;
        this.recipients = recipients;
    }

    public MessageContainer(String sender, Vector recipients, InputStream msg)
    throws MessagingException {
        this(sender, recipients, new MimeMessage(Session.getDefaultInstance(System.getProperties(), null), msg));
    }

    public MessageContainer(String sender, Vector recipients, MimeMessage message) {
        this(sender, recipients);
        this.message = message;
    }
    
    public MessageContainer duplicate() {
        MessageContainer response = new MessageContainer(sender, recipients, message);
        response.setMessageId(messageId);
        return response;
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

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessage(MimeMessage message) {
        this.message = message;
    }

    public void setMessage(InputStream in)
    throws MessagingException {
        this.setMessage(new MimeMessage(Session.getDefaultInstance(System.getProperties(), null), in));
    }

    public MimeMessage getMessage() {
        return message;
    }
}
