package org.apache.james.cheapospool;

import java.io.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import org.apache.james.*;
import org.apache.james.server.*;
import org.apache.james.util.*;
/**
 * This is a very primitive (and arguably effective) implementation of a MessageSpool.
 * @see org.apache.james.MessageSpool
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public class CheapoMessageSpool implements MessageSpool
{
	//Properties serverProps;
	JamesServ server = null;

	File spoolDir = null;

	public static final String messageExt = ".message";
	public static final String stateExt = ".state";
	public static final String checkedExt = ".checked";

	public static FilenameFilter stateFilter = null;
	public static FilenameFilter checkedFilter = null;

	/**
	 * CheapoMessageQueue constructor comment.
	 */
	public CheapoMessageSpool() {
		super();

		stateFilter = new CheapoExtensionFilter (stateExt);
		checkedFilter = new CheapoExtensionFilter (checkedExt);

	}

	/**
	 * addMessage method comment.
	 */
	public synchronized void addMessage(MimeMessage message, InternetAddress[] addresses)
	{
		DeliveryState state = new DeliveryState ();
		state.setRecipients (addresses);

		//We'll assume that the message-id is unique and just create two files appropriately
		saveMessage (message, state);
	}

	/**
	 * addMessage method comment.
	 */
	public synchronized void addMessage(MimeMessage message, DeliveryState state) throws MessagingException
	{
		if (message.getMessageID () == null)
			throw new MessagingException ("Message does not contain a Message-ID");

		//We'll assume that the message-id is unique and just create two files appropriately
		saveMessage (message, state);
	}

	/**
	 * This method was created in VisualAge.
	 * @param message javax.mail.internet.MimeMessage
	 */
	public synchronized void cancelChanges (MimeMessage message)
	{
		//We just have to reenable this message for checking
		uncheckMessage (message);
	}

	/**
	 * This method was created in VisualAge.
	 * @param msg javax.mail.internet.MimeMessage
	 * @param state org.apache.james.DeliveryState
	 */
	public synchronized void checkinMessage (MimeMessage msg, DeliveryState state)
	{
		//Do something, eh?
		uncheckMessage (msg);
		saveMessage (msg, state);
	}

	/**
	 * This renames the .state message to .checked indicating that the message
	 * is checked out.  It checks for existing messages and sleeps to handle
	 * file locking and other annoying bugs in at least the NT file system.
	 * @param message javax.mail.internet.MimeMessage
	 */
	private synchronized void checkMessage (MimeMessage message)
	{
		try
		{
			String filename = filenameSafeMessageID (message.getMessageID ());

			File temp = new File (spoolDir, filename + stateExt);
			File target = new File (spoolDir, filename + checkedExt);
			
			secureRename (temp, target);
		} catch (MessagingException me)
		{
			System.out.println ("Exception @ " + new Date ());
			me.printStackTrace (System.out);
		}
	}

	/**
	 * getMessage method comment.
	 */
	public synchronized MimeMessage checkoutMessage()
	{
		//If there are no messages that are available, return null
		String files[] = spoolDir.list (stateFilter);
		if (files.length == 0)
			return null;

		try
		{
			//Find the core filename (the message-id)
			String filename = files[0];
			filename = filename.substring (0, filename.length () - stateExt.length ());

			//Create the ServerMimeMessage
			File temp = new File (spoolDir, filename + messageExt);
			InputStream fin = new BufferedInputStream (new FileInputStream (temp));
			Session session = Session.getDefaultInstance(new Properties (), null);
			session.setDebug (false);
			MimeMessage message = new ServerMimeMessage (session, fin);
			fin.close ();

			//Rename the state file to checked so that it is checked out
			checkMessage (message);

			return message;
		} catch (IOException ioe)
		{
			System.out.println ("Exception @ " + new Date ());
			ioe.printStackTrace (System.out);
		} catch (MessagingException me)
		{
			System.out.println ("Exception @ " + new Date ());
			me.printStackTrace (System.out);
		}
		return null;
	}

	/**
	 * This method was created in VisualAge.
	 * @return java.lang.String
	 * @param messageID java.lang.String
	 */
	private static String filenameSafeMessageID (String messageID)
	{
		//We need to remove any offending characters from the Message-ID
		
		// NT invalid filename characters
		// ? " < > / \ : |
		
		String badChars = "?\"<>/\\:|";
		
		for (int i = 0; i < badChars.length (); i++)
			messageID = messageID.replace (badChars.charAt (i), '-');

		return messageID.trim ();
	}
	
	public synchronized DeliveryState getDeliveryState (MimeMessage message)
	{
		try
		{
			String filename = filenameSafeMessageID (message.getMessageID ());

			File temp = new File (spoolDir, filename + checkedExt);

			//Make sure the file exists
			if (!temp.exists ())
				return null;

			FileInputStream fin = new FileInputStream (temp);
			DeliveryState state = new DeliveryState (fin);
			fin.close ();

			return state;
		} catch (IOException ioe)
		{
			System.out.println ("Exception @ " + new Date ());
			ioe.printStackTrace (System.out);
		} catch (MessagingException me)
		{
			System.out.println ("Exception @ " + new Date ());
			me.printStackTrace (System.out);
		}
		return null;
	}
	
	/**
	 * This method was created in VisualAge.
	 * @return boolean
	 */
	public synchronized boolean hasMessages ()
	{
		return spoolDir.list (stateFilter).length > 0;
	}
	
	/**
	 * setProperties method comment.
	 */
	public synchronized void init (JamesServ server)
	{
		this.server = server;

		spoolDir = new File (server.getProperty ("spool.dir"));
		if (!spoolDir.isDirectory ())
		{
			System.out.println ("The spool.dir setting is not a directory.");
			System.exit (-1);
		}

		//We should convert all checked out messages to normal state messages
		String files[] = spoolDir.list (checkedFilter);
		for (int i = 0; i < files.length; i++)
		{
			String filename = files[i];
			filename = filename.substring (0, filename.length () - checkedExt.length ());
			File temp = new File (spoolDir, filename + checkedExt);
			File target = new File (spoolDir, filename + stateExt);
			
			secureRename (temp, target);
		}
	}
	
	/**
	 * This method was created in VisualAge.
	 * @param message javax.mail.internet.MimeMessage
	 */
	public synchronized void removeMessage (MimeMessage message)
	{
		try
		{
			String filename = filenameSafeMessageID (message.getMessageID ());

			String files[] = spoolDir.list (new CheapoFilenameFilter (filename));

			for (int i = 0; i < files.length; i++)
			{
				File temp = new File (spoolDir, files[i]);

				while (!temp.delete ())
					sleep (50);
			}
		} catch (MessagingException me)
		{
			System.out.println ("Exception @ " + new Date ());
			me.printStackTrace (System.out);
		}
	}
	
	/**
	 * This method was created in VisualAge.
	 * @param message javax.mail.internet.MimeMessage
	 * @param state org.apache.james.DeliveryState
	 */
	private synchronized void saveMessage (MimeMessage message, DeliveryState state)
	{
		try
		{
			String filename = filenameSafeMessageID (message.getMessageID ());

			File targetBody = new File (spoolDir, filename + messageExt);
			File targetState = new File (spoolDir, filename + stateExt);

			File tempBody = new File (spoolDir, filename + ".temp" + messageExt);
			File tempState = new File (spoolDir, filename + ".temp" + stateExt);

			FileOutputStream fout = new FileOutputStream (tempBody);
			message.writeTo (fout);
			fout.close ();

			fout = new FileOutputStream (tempState);
			state.writeTo (fout);
			fout.close ();
			
			secureRename (tempState, targetState);
			secureRename (tempBody, targetBody);

		} catch (IOException ioe)
		{
			System.out.println ("Exception @ " + new Date ());
			ioe.printStackTrace (System.out);
		} catch (MessagingException me)
		{
			System.out.println ("Exception @ " + new Date ());
			me.printStackTrace (System.out);
		}

	}

	/**
	 * Renames a 
	 * @param message javax.mail.internet.MimeMessage
	 */
	private synchronized void uncheckMessage (MimeMessage message)
	{
		try
		{
			String filename = filenameSafeMessageID (message.getMessageID ());

			File temp = new File (spoolDir, filename + checkedExt);
			File target = new File (spoolDir, filename + stateExt);
			
			secureRename (temp, target);
			
		} catch (MessagingException me)
		{
			System.out.println ("Exception @ " + new Date ());
			me.printStackTrace (System.out);
		}
	}
	
	/**
	 * Safely renames the file objects.  NT seems to have trouble releasing
	 * file handles which can abort a rename (if the file it is renaming to
	 * still exists.
	 */
	
	private void secureRename (File source, File target)
	{
		while (target.exists ())
		{
			target.delete ();
			sleep (50);
		}

		while (!source.renameTo (target))
			sleep (50);

		while (source.exists ())
			sleep (50);
	}
	/**
	 * Simple routine to sleep, catching exceptions
	 * @param long ms Number of milliseconds to sleep
	 */
	private void sleep (long ms)
	{
		System.out.println ("Sleeping for 50s...");	
		try
		{
			Thread.sleep (ms);
		} catch (InterruptedException ie)
		{}
	}
}