package org.apache.james.james.servlet;

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
import org.apache.arch.*;
import org.apache.james.*;
import org.apache.mail.*;
import org.apache.avalon.blocks.*;
import javax.mail.Transport;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.*;

import org.xbill.DNS.*;

/**
 * Receive  a MessageContainer from JamesSpoolManager and takes care of delivery 
 * the message to remote hosts. If for some reason mail can't be delivered
 * store it in the "delayed" Repository and set an Alarm. After "delayTime" the 
 * Alarm will wake the servlet that will try to send it again. After "maxRetyes"
 * the mail will be considered underiverable and will be returned to sender.
 *
 * Note: Many FIXME on the air.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class RemoteDelivery extends GenericMailServlet implements TimeServer.Bell {

	private MessageContainerRepository delayed;
	private Context context;
	private Transport loopBack;
	private TimeServer timeServer;
	private long delayTime;
	private int maxRetyes;

	private Resolver resolver;
	private Cache cache;
	private byte dnsCredibility;
	private InternetAddress postmaster;

	public void init () 
	throws Exception {
		ComponentManager comp = getComponentManager();
		String delayedRepository = getConfiguration("repository").getValue();
		delayTime = getConfiguration("delayTime", "21600000").getValueAsLong(); // default is 6*60*60*1000 mills
		maxRetyes = getConfiguration("maxRetyes", "5").getValueAsInt(); // default is 5 retryes
		Store store = (Store) comp.getComponent(Interfaces.STORE);
		delayed = (MessageContainerRepository) store.getPrivateRepository(delayedRepository, MessageContainerRepository.MESSAGE_CONTAINER, Store.ASYNCHRONOUS);
		timeServer = (TimeServer) comp.getComponent(Interfaces.TIME_SERVER);
		int i = 0;
		for (Enumeration e = delayed.list(); e.hasMoreElements(); ) {
			String key = (String) e.nextElement();
			timeServer.setAlarm(key, this, ++i * 10000);
			log("delayed message " + key + " set for delivery in " + (i * 10) + " seconds");
		}
		URLName urlname = new URLName("smtp://localhost");
		loopBack = Session.getDefaultInstance(System.getProperties(), null).getTransport(urlname);

		Vector dnsServers = new Vector();
		for (Enumeration e = getConfigurations("DNSservers.server") ; e.hasMoreElements(); ) {
		    dnsServers.addElement(e.nextElement());
		}
		String servers[] = new String [dnsServers.size()];
		i = 0;
		for (Enumeration e = dnsServers.elements() ; e.hasMoreElements(); i++) {
		    Configuration c = (Configuration) e.nextElement();
		    servers[i] = c.getValue();
		}
		resolver = new ExtendedResolver (servers);
        boolean authoritative = getConfiguration("authoritative", "false").getValueAsBoolean();
		dnsCredibility = authoritative ? Credibility.AUTH_ANSWER : Credibility.NONAUTH_ANSWER;

		postmaster = new InternetAddress (getConfiguration("postmaster", "root@localhost").getValue());
	}




	/**
	 * We can assume that the recipients of this message are all going to the same
	 * mail server.  We need to determine what this mail server is, query MX records (and maybe A)
	 * for this server name, get a list sorted by priority, and try all of them.  If it
	 * fails all the DNS lookup, mark this and put it [back] in the delayed queue.  If it
	 * fails all attempts to deliver, mark this and put it [back] in the delayed queue.
	 * 
	 * Creation date: (2/24/00 11:25:00 PM)
	 * @param mc org.apache.mail.MessageContainer
	 */
	private void deliver(MessageContainer mc) {
		Vector recipients = mc.getRecipients ();
		Vector targetServers = new Vector ();

		try {
			//Need to lookup the possible ip addresses to connect to...
			String tempRcpt = (String)recipients.elementAt (0);
			String serverName = tempRcpt.substring (tempRcpt.indexOf ("@") + 1);
			targetServers = dnsLookup (serverName);
		} catch (Exception e) {
			//We failed to find domain information... fail this message accordingly
			failMessage (mc, "DNS lookup failed");
			return;
		}
		Exception lastException = null;
		for (int i = 0; i < targetServers.size (); i++) {
			try {
				String outgoingmailserver = (String)targetServers.elementAt (i);
				URLName urlname = new URLName("smtp://" + outgoingmailserver);

				Transport transport = Session.getDefaultInstance(System.getProperties(), null).getTransport(urlname);

				InternetAddress addr[] = new InternetAddress[recipients.size()];
				int j = 0;
				for (Enumeration e = recipients.elements(); e.hasMoreElements(); j++) {
					addr[j] = new InternetAddress((String) e.nextElement());
				}
				transport.connect();
				transport.sendMessage(mc.getMessage(), addr);
				transport.close();
				//We've succeeded somehow!!!  Cheers!
				return;
			} catch (Exception ex) {
				//We failed on connect... we'll keep trying

				//We should do a better job checking this... if the failure is a general
				//connect exception, this is less descriptive than more specific SMTP command
				//failure... have to lookup and see what are the various Exception
				//possibilities
				lastException = ex;
			}
		}
		//Unable to deliver message after numerous tries... fail accordingly
		failMessage (mc, "Delivery failure: " + lastException.toString ());
	}
	public void destroy() {
	}
/**
 * Insert the method's description here.
 * Creation date: (2/25/00 12:52:33 AM)
 * @return java.lang.String[]
 * @param name java.lang.String
 */
private Vector dnsLookup(String name) {
	Record answers[] = rawDNSLookup(name, false);

	//Sort the records by MX priority
	MXRecord mxAnswers[] = new MXRecord[answers.length];
	for (int i = 0; i < answers.length; i++) {
		mxAnswers[i] = (MXRecord) answers[i];
	}
	for (int i = 0; i < mxAnswers.length; i++) {
		for (int j = i + 1; j < mxAnswers.length; j++) {
			if (mxAnswers[j].getPriority() < mxAnswers[i].getPriority()) {
				MXRecord temp = mxAnswers[j];
				mxAnswers[j] = mxAnswers[i];
				mxAnswers[i] = temp;
			}
		}
	}
	Vector servers = new Vector ();
	for (int i = 0; i < mxAnswers.length; i++) {
		servers.addElement (mxAnswers[i].getTarget ());
	}
	return servers;
}
/**
 * Insert the method's description here.
 * Creation date: (2/25/00 1:14:18 AM)
 * @param mc org.apache.mail.MessageContainer
 * @param reason java.lang.String
 */
private void failMessage(MessageContainer mc, String reason) {
	log("Exception delivering mail: " + reason);
	if (!mc.getState().equals(MessageContainer.ERROR)) {
		mc.setState(MessageContainer.ERROR);
		mc.setErrorMessage("1");
	}
	int retries = Integer.parseInt(mc.getErrorMessage());
	if (retries > maxRetyes) {
		log("Sending back message " + mc.getMessageId() + " after " + retries + " retries");
		try {
			//FIXME: need much better logging of why this message failed, including the
			//original message as an attachment.  q.q.v. old james stuff.
			MimeMessage reply = (MimeMessage) (mc.getMessage()).reply(false);
			reply.setSubject("Unable to deliver this message to recipients: " + reason);
			InternetAddress addr[] = {new InternetAddress(mc.getSender())};
			reply.setRecipients(Message.RecipientType.TO, addr);
			reply.setFrom(postmaster);

			//FIXME: Can't we just do sendMail on the James server object?  Shoudn't need to connect
			loopBack.connect();
			loopBack.sendMessage(reply, addr);
			loopBack.close();
		} catch (Exception ignore) {
			// FIXME: cannot destroy mails... what should we do here ?
			log("Unable to reply. Destroying message");
			//This should go to straight to the postmaster... but I'm sleepy
		}
	} else {
		log("Storing message " + mc.getMessageId() + " into delayed after " + retries + " retries");
		mc.setErrorMessage("" + ++retries);
		delayed.store(mc.getMessageId(), mc);
		timeServer.setAlarm(mc.getMessageId(), this, delayTime);
	}
}
	public String getServletInfo() {
		return "RemoteDelivery Mail Servlet";
	}
/**
 * Insert the method's description here.
 * Creation date: (2/24/00 11:45:56 PM)
 * @return org.xbill.DNS.Record[]
 * @param name java.lang.String
 */
private Record[] rawDNSLookup(String namestr, boolean querysent) {
	Name name = new Name(namestr);
	short type = Type.MX;
	short dclass = DClass.IN;
	
	Record [] answers;
	int answerCount = 0, n = 0;
	Enumeration e;

	SetResponse cached = cache.lookupRecords(name, type, dclass, dnsCredibility);
	if (cached.isSuccessful()) {
		RRset [] rrsets = cached.answers();
		answerCount = 0;
		for (int i = 0; i < rrsets.length; i++)
			answerCount += rrsets[i].size();

		answers = new Record[answerCount];

		for (int i = 0; i < rrsets.length; i++) {
			e = rrsets[i].rrs();
			while (e.hasMoreElements()) {
				Record r = (Record)e.nextElement();
				answers[n++] = r;
			}
		}
	}
	else if (cached.isNegative()) {
		return null;
	}
	else if (querysent) {
		return null;
	}
	else {
		Record question = Record.newRecord(name, type, dclass);
		org.xbill.DNS.Message query = org.xbill.DNS.Message.newQuery(question);
		org.xbill.DNS.Message response;

		try {
			response = resolver.send(query);
		}
		catch (Exception ex) {
			return null;
		}

		short rcode = response.getHeader().getRcode();
		if (rcode == Rcode.NOERROR || rcode == Rcode.NXDOMAIN)
			cache.addMessage(response);

		if (rcode != Rcode.NOERROR)
			return null;

		return rawDNSLookup(namestr, true);
	}

	return answers;
}
	/**
	 * For this message, we take the list of recipients, organize these into distinct
	 * servers, and duplicate the message for each of these servers, and then call
	 * the deliver (messagecontainer) method for each server-specific
	 * messagecontainer ... that will handle storing it in the delayed queue if needed.
	 * 
	 * @param mc org.apache.mail.MessageContainer
	 * @return org.apache.mail.MessageContainer
	 */
	public MessageContainer service(MessageContainer mc) {
		log("Remotly delivering mail " + mc.getMessageId());
		Vector recipients = mc.getRecipients();

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
		//delayed.remove (mc.getMessageId ());
		
		//store the new message containers - organized by server
		for (Enumeration e = targets.keys(); e.hasMoreElements ();) {
			String targetServer = (String)e.nextElement ();
			MessageContainer subMc = mc.duplicate ();
			//Trim the recipients down to only ones of this server
			recipients = subMc.getRecipients ();
			int i = 0;
			while (i < recipients.size ()) {
				String recipient = (String)recipients.elementAt (i);
				if (recipient.toLowerCase ().endsWith ("@" + targetServer)) {
					i++;
				} else {
					recipients.removeElementAt (i);
				}
			}
			subMc.setMessageId (subMc.getMessageId () + "-to-" + targetServer);
			deliver (subMc);
		}

		return (MessageContainer) null;
	}

	public void wake(String name, String memo) {
		MessageContainer mc = delayed.retrieve(name);
		delayed.remove(name);
		deliver(mc);
	}
}
