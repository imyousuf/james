package com.lokitech.mail;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

import org.apache.james.*;
import org.apache.james.util.*;

/**
 * A mail message logger
 * by Serge Knystautas
 * Copyright (C) 1999 Loki Technologies
 * 
 * A very simple servlet, this stores each mail message and it's delivery information to a specified
 * directory.  It stores messages based on their message-ID, so be sure multiple instances of the
 * log servlet do not use the same directory.  Specify what directory to store files in using the
 * confFile servlet parameter.
 * @author Serge Knystautas <a href="mailto:sergek@lokitech.com">sergek@lokitech.com</a>
 * @version 1.0
 */
public class LogServlet extends MailServlet
{
	File logDir = null;			//The directory where messages are logged to
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 * @param messageID java.lang.String
 */
private static String filenameSafeMessageID (String messageID)
{
	//We need to remove any offending characters from the Message-ID
	
	// ? " < > / \ : |
	
	//For now, we'll be pretty relaxed in our changes
	return messageID.replace ('<', ' ').replace ('>', ' ').trim ();
}
/**
 * This method was created in VisualAge.
 * @param config org.apache.james.MailServletConfig
 */
public void init (MailServletConfig config) throws MessagingException
{
	super.init (config);

	File file = config.getConfFile ();
	if (!file.exists ())
	{
		log ("conf file does not exist");
		throw new MessagingException ("Bad conf file");
	}
	if (!file.isDirectory ())
	{
		log ("conf file does not specify a directory");
		throw new MessagingException ("Conf file not directory");
	}

	logDir = file;
}
/**
 * service method comment.
 */
public void service(MimeMessage message, DeliveryState state) throws IOException, MessagingException
{
	//Log the message in the appropriate directory (this is based off of code used in the
	//CheapoMessageSpool
	try
	{
		String filename = filenameSafeMessageID (message.getMessageID ());

		File temp = new File (logDir, filename + ".message");
		FileOutputStream fout = new FileOutputStream (temp);
		message.writeTo (fout);
		fout.close ();

		temp = new File (logDir, filename + ".state");
		fout = new FileOutputStream (temp);
		PrintWriter pout = new PrintWriter (fout);
		pout.println ("log.date=" + RFC822DateFormat.toString (new Date ()));
		pout.flush ();
		state.writeTo (fout);
		fout.close ();
	} catch (IOException ioe)
	{
		System.out.println ("Exception @ " + new Date ());
		ioe.printStackTrace (System.out);
	} catch (MessagingException me)
	{
		me.printStackTrace ();
	}
	
}
}