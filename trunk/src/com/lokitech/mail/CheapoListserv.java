package com.lokitech.mail;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

import org.apache.james.*;
/**
 * A cheapo listserv servlet.  All we do is change who are set as the recipients of the email address.
 * by Serge Knystautas
 * Copyright (C) 1999 Loki Technologies
 *
 * This adds [listserv name] to the subject, and changes who the message is being sent to.
 * The conf file should be a list of email addresses like:
 <PRE>
sergek@lokitech.com
jon.stevens@lokitech.com
</PRE>
  Addresses can optionally contain <>.
  <br>
  <br>
  You need to set the address parameter so the servlet knows what email should it intercept and
  redistribute. You can optionally set the following parameters:
  <ul>
  <li>subject - added to the subject field of the messages</li>
  <li>recipientsOnly - limit the senders to those on the recipient list.</li>
  <li>attachments - whether attachments are allowed</li>
  For example:
  <PRE>
processor.12.code=com.lokitech.mail.CheapoListserv
processor.12.confFile=c:/Java/James/aliases.conf
processor.12.initArgs=address=linux@lokitech.com,subject=Servlets,recipientsOnly=false, attachments=false
</PRE>
 * @author Serge Knystautas <a href="mailto:sergek@lokitech.com">sergek@lokitech.com</a>
 * @version 1.0
 */
public class CheapoListserv extends MailServlet
{
	//This contains the addresses in the recipients
	private Vector recipients = null;
	//The subject for listserv traffic
	private String subject = null;
	//Whether only recipients can send messages to this listserv
	private boolean recipientsOnly = false;
	//Whether this listserv supports sending attachments
	private boolean attachments = true;
	//The email address that this list serv works on
	private InternetAddress listservAddress = null;
/**
 * This method was created in VisualAge.
 * @param config org.apache.james.MailServletConfig
 */
public void init (MailServletConfig config) throws MessagingException
{
	super.init (config);

	try
	{
		if (config.getInitParameter ("address") == null)
		{
			log ("address not specified");
			throw new MessagingException ("address not specified.");
		}
		listservAddress = new InternetAddress (config.getInitParameter ("address"));

		File file = config.getConfFile ();
		recipients = new Vector ();
		
		if (file != null && file.exists ())
		{
			BufferedReader reader = new BufferedReader (new FileReader (file));
			String line = null;
			int count = 0;
			while ((line = reader.readLine ()) != null)
			{
				count++;
				if (line.startsWith ("#"))
					continue;
				
				InternetAddress addr[] = InternetAddress.parse (line);
				if (addr.length != 1)
				{
					System.out.println ("line " + count + " does not contain a proper address");
					continue;
				}
				recipients.addElement (addr[0]);
			}
		}

		if (config.getInitParameter ("subject") != null)
			subject = '[' + config.getInitParameter ("subject") + ']';
		if (config.getInitParameter ("recipientsOnly") != null)
			recipientsOnly = config.getInitParameter ("recipientsOnly").equals ("true");
		if (config.getInitParameter ("attachments") != null)
			attachments = config.getInitParameter ("attachments").equals ("true");
	} catch (IOException ioe)
	{
		System.out.println ("Exception @ " + new Date ());
		ioe.printStackTrace (System.out);
	}
}
/**
 * service method comment.
 */
public void service(MimeMessage message, DeliveryState state) throws IOException, MessagingException
{
	InternetAddress addr[] = state.getRecipients();
	for (int i = 0; i < addr.length; i++)
	{
		if (addr[i].equals (listservAddress))
		{
			//Check for recipients only....
			if (recipientsOnly)
			{
				//later...
			}
			
			//Check for no attachments
			if (!attachments)
			{
				//later...
			}
				
			//Do stuff
			Vector newRcpts = new Vector ();

			//Put all the other recipients into a new Vector of recipients
			for (int j = 0; j < addr.length; j++)
				if (j != i)
					newRcpts.addElement (addr[j]);

			//Add everyone on the listserv's recipient list
			for (int j = 0; j < recipients.size (); j++)
				if (!newRcpts.contains (recipients.elementAt (j)))
					newRcpts.addElement (recipients.elementAt (j));

			//Convert the vector into an array
			addr = new InternetAddress[newRcpts.size ()];
			for (int j = 0; j < newRcpts.size (); j++)
				addr[j] = (InternetAddress)newRcpts.elementAt (j);

			//Set the new recipient list
			state.setRecipients (addr);

			//Set the subject if so
			if (subject != null)
			{
				String subj = message.getSubject ();
				//If the "subject" is in the subject line, remove it and everything before it
				int index = subj.indexOf (subject);
				if (index > -1)
				{
					if (index == 0)
						subj = subject + ' ' + subj.substring (index + subject.length () + 1);
					else
						subj = subject + ' ' + subj.substring (0, index) + subj.substring (index + subject.length () + 1);
				} else
					subj = subject + ' ' + subj;
				
				message.setSubject (subj);
			}
			message.setHeader ("Reply-To", listservAddress.toString ());
			
			state.setState (DeliveryState.PROCESSED);
			return;
		}
	}
}
}