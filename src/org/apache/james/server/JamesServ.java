package org.apache.james.server;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

import org.apache.james.*;
import org.apache.james.util.*;

/**
 * <b>Run this class!!</b>
 * Pass it the name of a conf file.  This will be used to configure the server daemon, the spool, and
 * the mail servlets themselves.  Hope it works for you!
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public class JamesServ extends Thread
{
	private Properties props;
	//private static String confFile;
	private JamesSpoolManager spoolMgr = null;

	protected static int mcount = 0;
/**
 * This method was created in VisualAge.
 * @param props java.util.Properties
 */
public JamesServ (String confFile)
{
	super ("JamesServ socket listener");
	//Set instance settings
	props = new Properties ();

	try
	{
		FileInputStream in = new FileInputStream (confFile);
		props.load (in);
		in.close ();
	} catch (IOException ioe)
	{
		System.out.println ("Exception @ " + new Date ());
		ioe.printStackTrace (System.out);
	}

	if (props.getProperty ("server.name") == null)
	{
		InetAddress serverHost = null;;
		try
		{
			serverHost = InetAddress.getLocalHost ();
			props.put ("server.name", serverHost.getHostName ());
		} catch (UnknownHostException e)
		{
			e.printStackTrace ();
		}
	}
	
	//Create a new spool manager
	spoolMgr = new JamesSpoolManager (this);

	int threads = 1;
	try
	{
		threads = Integer.parseInt (props.getProperty ("spool.threads"));
	} catch (Exception e)
	{}
	
	while (threads-- > 0)
	{
		new Thread (spoolMgr, "James spoolmgr " + (threads + 1)).start ();
	}
	
}
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 */
public String getMessageID ()
{
	int mid = ++mcount;
	Date msgTime = new Date ();
	return msgTime.getTime () + "." + mid + "@" + getProperty ("server.name");
}
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 * @param name java.lang.String
 */
public String getProperty (String name)
{
	return props.getProperty (name);
}
/**
 * This method was created in VisualAge.
 * @return org.apache.james.MessageSpool
 */
public MessageSpool getSpool () {
	return spoolMgr.getSpool ();
}
/**
 * This is the main entry point of the James server.  It initializes the spool, starts listening
 * on port 25, and passes connections to JamesConnection objects.
 * @param arg java.lang.String[]
 */
public static void main (String arg[])
{
	if (arg.length != 1)
	{
		System.out.println ("usage: java org.apache.james.JamesServ configFile");
	} else
	{
		JamesServ server = new JamesServ (arg[0]);
		server.start ();
	}
}
/**
 * This method was created in VisualAge.
 */
public void run ()
{
	ServerSocket ss = null;
	try
	{
		ss = new ServerSocket (25);
	} catch (IOException e)
	{
		System.out.println ("Exception @ " + new Date ());
		e.printStackTrace (System.out);
		return;
	}
	Socket s = null;
	System.out.println ("MXServ is running");
	while (true)
	{
		try
		{
			s = ss.accept ();

			s.setSoTimeout (120000);	//set timeout to 2 minutes
			JamesConnection handler = new JamesConnection (s, this);
			new Thread (handler).start ();
		} catch (SocketException se)
		{
			System.out.println ("Socket closed.");
		} catch (IOException e)
		{
			System.out.println ("Exception @ " + new Date ());
			e.printStackTrace (System.out);
		}
	}

}
}