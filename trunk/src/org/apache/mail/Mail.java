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
 * Wrap a MimeMessage adding routing informations (from SMTP) and same simple API. 
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class Mail implements Serializable, Cloneable {

	public final static String GHOST = "GHOST";
	public final static String DEFAULT = "DEFAULT";
	public final static String ERROR = "ERROR";
	public final static String TRANSPORT = "TRANSPORT";

	private String errorMessage;
	private String state;
	private MimeMessage message;
	private InputStream messageIn;
	private String sender;
	private Vector recipients;
	private String name;


	public Mail() {
	    setState(DEFAULT);
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
	public Mail duplicate(String newName) {
		try {
			return new Mail(newName, new String(sender), new Vector(recipients), getMessage());
		} catch (MessagingException me) {
		}
		return (Mail) null;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public MimeMessage getMessage() 
	throws MessagingException {
		parse();
		return message;
	}
	
	public void setName(String name) {
	    this.name = name;
	}
	
	public String getName() {
		return name;
	}
	public Vector getRecipients() {
		return recipients;
	}
	public String getSender() {
		return sender;
	}
	public String getState() {
		return state;
	}
	public void parse() 
	throws MessagingException {
		if (messageIn != null) {
			message = new MimeMessage(Session.getDefaultInstance(System.getProperties(), null), messageIn);
			messageIn = null;
		}
	}
	private void readObject(java.io.ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		sender = (String) in.readObject();
		recipients = (Vector) in.readObject();
		state = (String) in.readObject();
		errorMessage = (String) in.readObject();
		name = (String) in.readObject();
	}
	public void setErrorMessage(String msg) {
		this.errorMessage = msg;
	}
	public void setMessage(InputStream in)
	throws MessagingException {
		this.messageIn = new BufferedInputStream(in);
		this.message = null;
	}
	public void setMessage(MimeMessage message) {
		this.message = message;
		this.messageIn = null;
	}
	public void setRecipients(Vector recipients) {
		this.recipients = recipients;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public void setState(String state) {
		this.state = state;
	}
	public void writeMessageTo(OutputStream out)
	throws IOException, MessagingException {
		if (message != null) {
			message.writeTo(out);
		} else {
// FIXME!!! need a smarter way to read many times the same stream!!!            
			messageIn.mark(Integer.MAX_VALUE);
			for (int next; (next = messageIn.read()) != -1; out.write(next));
			messageIn.reset();
		}
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
	
    public static String getUser(String recipient) {
        return recipient.substring(0, recipient.indexOf("@"));
    }

    public static String getHost(String recipient) {
        return recipient.substring(recipient.indexOf("@") + 1);
    }

    public Mail bounce(String message) throws MessagingException {

        //This sends a message to the james component that is a bounce of the sent message
        MimeMessage original = getMessage();
        MimeMessage reply = (MimeMessage) original.reply(false);
        reply.setSubject("Re: " + original.getSubject());
        Vector recipients = new Vector();
        recipients.addElement(getSender());
        InternetAddress addr[] = {new InternetAddress(getSender())};
        reply.setRecipients(Message.RecipientType.TO, addr);
        reply.setFrom(new InternetAddress(getRecipients().elementAt(0).toString()));
        reply.setText(message);
        reply.setHeader("Message-Id", "replyTo:" + getName());

        return new Mail("replyTo" + getName(), getRecipients().elementAt(0).toString(), recipients, reply);
    }


}
