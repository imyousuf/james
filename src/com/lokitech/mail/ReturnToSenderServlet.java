package com.lokitech.mail;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.james.*;
/**
 * A mail alias mail servlet.
 * by Serge Knystautas
 * Copyright (C) 1999 Loki Technologies
 * 
 * This is the standard return a message to a sender which should be the default action for
 * the failure.processor in the mail server.  Right now this does not support delayed delivery
 * or anything that complicated.  If the target mail server does not respond or if there are other
 * delays, it immediately returns the message to the server.  This will need to be corrected
 * at some point.
 *
 * @author Serge Knystautas <a href="mailto:sergek@lokitech.com">sergek@lokitech.com</a>
 * @version 1.0
 */
public class ReturnToSenderServlet extends MailServlet
{
/**
 * service method comment.
 */
public void service(MimeMessage message, DeliveryState state) throws IOException, MessagingException
{
	if (state.getState () != DeliveryState.FAILED_DELIVERY)
		return;

	//We want to generate a new message that contains the existing message and return that
	//to the sender

	Session session = Session.getDefaultInstance(System.getProperties(), null);
	session.setDebug (false);

	int failureCount = Integer.parseInt (state.getProperty ("failure.count"));
	for (int i = 0; i < failureCount; i++)
	{

		MimeMessage newMsg = new MimeMessage (session);
		DeliveryState newState = new DeliveryState ();
		
		//We want this message to be immediately sent out without having to process anything
		newState.setState (DeliveryState.NOT_PROCESSED);
		
		//Set the target of the returned email message to either the Return-Path or the From header
		String sender = null;
		if (message.getHeader ("Return-Path", ",") != null)
			sender = message.getHeader ("Return-Path", ",");
		else
			sender = message.getHeader ("From", ",");
		if (sender == null)
			throw new MessagingException ("Could not find sender in bad message.");
		InternetAddress addr[] = InternetAddress.parse(sender);
		newState.setRecipients (addr);
		
		//Create the multiple body parts
		Multipart mp = new MimeMultipart("report; report-type=delivery-status");

		// create and fill the first message part
		MimeBodyPart mbp = new MimeBodyPart();
		ByteArrayOutputStream bout = new ByteArrayOutputStream ();
		PrintWriter out = new PrintWriter (bout);

		String failureReason = state.getProperty ("failure." + i + ".reason");
		System.out.println ("**Determined failure was " + failureReason);
		out.println ("This Message was undeliverable due to the following reason:");
		out.println ();
		if ((DeliveryState.HOST_NOT_FOUND + "").equals (failureReason))
		{
			//Host not found
			out.println ("Your message was not delivered because the destination computer was");
			out.println ("not found.  Carefully check that it was spelled correctly and try");
			out.println ("sending it again if there were any mistakes.");
			out.println ();
			out.println ("It is also possible that a network problem caused this situation,");
			out.println ("so if you are sure the address is correct you might want to try to");
			out.println ("send it again.  If the problem continues, contact your");
			out.println ("system administrator.");
			out.println ();
			out.println ("     Host " + state.getProperty ("failure." + i + ".host") + " not found");
		} else if ((DeliveryState.INVALID_RECIPIENT + "").equals (failureReason))
		{
			//Invalid recipient
			out.println ("Your message was not delivered because the destination computer did");
			out.println ("not like the recipient.  Carefully check that it was spelled correctly and try");
			out.println ("sending it again if there were any mistakes.");
			out.println ();
			out.println ("     Remote host said: " + state.getProperty ("failure." + i + ".desc"));
		} else if ((DeliveryState.UNKNOWN_HOST + "").equals (failureReason))
		{
			//Host not found
			out.println ("Your message was not delivered because the destination computer was");
			out.println ("not found.  Carefully check that it was spelled correctly and try");
			out.println ("sending it again if there were any mistakes.");
			out.println ();
			out.println ("It is also possible that a network problem caused this situation,");
			out.println ("so if you are sure the address is correct you might want to try to");
			out.println ("send it again.  If the problem continues, contact your");
			out.println ("system administrator.");
			out.println ();
			out.println ("     Host " + state.getProperty ("failure." + i + ".host") + " not found");
		} else if ((DeliveryState.ACCOUNT_NOT_FOUND + "").equals (failureReason))
		{
			//Local Loopback Exception
			out.println ("Your message was not delivered because that account is not recognized");
			out.println ("by this server.  This created a loop, which was aborted.");
		} else if ((DeliveryState.UNKNOWN_FAILURE + "").equals (failureReason))
		{
			//Generic message
			out.println ("This is a generic message failure.  We have not categorized this failure,");
			out.println ("but in any case, your message was not delivered.  Please try again later");
			out.println ("or contact your system administrator.");
		}
		out.println ();
		out.println ("The following recipients did not receive this message:");
		out.println ();
		addr = InternetAddress.parse (state.getProperty ("failure." + i + ".addr"));
		out.print ("     ");
		for (int j = 0; j < addr.length; j++)
		{
			out.print ('<' + addr[j].toString () + '>');
			if (j < addr.length - 1)
				out.print (", ");
		}
		out.println ();
		out.println ();
		out.println ("Please reply to " + getContext ().getProperty ("postmaster"));
		out.println ("if you feel this message to be in error.");
		out.flush ();
		mbp.setText(bout.toString (), "us-ascii");

		mbp.addHeader ("Content-Type", "text/plain; charset=us-ascii");
		mbp.addHeader ("Content-Transfer-Encoding", "7bit");

		mp.addBodyPart(mbp);
		
		// create and fill the second message part
		//mbp = new MimeBodyPart();
		
		// Use setText(text, charset), to show it off !
		//mbp.setText("I'll do some fancy header stuff later.....", "us-ascii");
		//mbp.addHeader ("Content-Type", "message/delivery-status");
		//mbp.addHeader ("Content-Disposition", "inline");
		//mbp.addHeader ("Content-Transfer-Encoding", "7bit");
		//mp.addBodyPart(mbp);
		
		//create and fill the third message part (this is in fact the actual message)
		mbp = new MimeBodyPart();

		bout = new ByteArrayOutputStream ();
		message.writeTo (bout);
		mbp.setText (bout.toString ());

		mbp.addHeader ("Content-Type", "message/rfc822");
		mbp.addHeader ("Content-Disposition", "inline");
		mbp.addHeader ("Content-Transfer-Encoding", "7bit");
		mp.addBodyPart(mbp);
		System.out.println ("sections..." + mp.getContentType ());

		//Since it can't behave itself, we're going to try to set this manually
		String contentType = mp.getContentType ();
		int index = contentType.indexOf ("boundary");
		if (index > -1)
			contentType = contentType.substring (0, index) + "B" + contentType.substring (index + 1);
			
		//mp.writeTo (System.out);
		//newMsg.setText (mp);
		//newMsg.setText ("screwed up");
		newMsg.setContent (mp);
		

			//Set a bunch of headers
		//newMsg.addHeader ("Return-Path", "<>");
		newMsg.setRecipients(Message.RecipientType.TO, addr);
		newMsg.addHeader ("From", "Mail Administrator <" + getContext ().getProperty ("postmaster") + ">");
		newMsg.addHeader ("Reply-To", "Mail Administrator <" + getContext ().getProperty ("postmaster") + ">");
		newMsg.addHeader ("Subject", "Mail System Error - Returned Mail");
		newMsg.addHeader ("Date", org.apache.james.util.RFC822DateFormat.toString (new Date ()));
		newMsg.addHeader ("Message-ID", "<" + getContext ().getMessageID () + ">");
		newMsg.addHeader ("MIME-Version", "1.0");

		newMsg.setHeader ("Content-Type", contentType);

		getContext ().sendMessage (newMsg, newState);
	}
}
}