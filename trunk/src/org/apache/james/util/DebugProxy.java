package org.apache.james.util;

import java.io.*;
import java.net.*;
import java.util.*;
/**
 * This type was created in VisualAge.
 */
public class DebugProxy extends Thread
{
	InputStream in;
	OutputStream out;

	int data = 0;
/**
 * DebugProxy constructor comment.
 * @parameter java.io.InputStream stream to read from
 * @parameter java.io.OutputStream stream to write to
 */
public DebugProxy(InputStream in, OutputStream out)
{
	this.in = in;
	this.out = out;
	
	start ();
}
/**
 * This method was created in VisualAge.
 * @param args java.lang.String[]
 */
public static void main(String args[])
{
	if (args.length != 3)
	{
		System.err.println ("Format:");
		System.err.println ("  java org.apache.james.util.DebugProxy <listen_port> <target_server> <target_port>");
		return;
	}
	int listenPort = Integer.parseInt (args[0]);
	String target = args[1];
	int targetPort = Integer.parseInt (args[2]);
	
	ServerSocket ss = null;
	try
	{
		System.out.println ("Listening to " + listenPort);
		ss = new ServerSocket (listenPort);
	} catch (IOException e)
	{
		System.out.println ("Exception @ " + new Date ());
		e.printStackTrace (System.out);
		return;
	}
	Socket s = null;
	Socket t = null;
	while (true)
	{
		try
		{
			s = ss.accept ();
			s.setSoTimeout (120000);	//set timeout to 2 minutes

			t = new Socket (target, targetPort);
			
			new DebugProxy (t.getInputStream (), s.getOutputStream ());
			new DebugProxy (s.getInputStream (), t.getOutputStream ());
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
/**
 * This method was created in VisualAge.
 */
public void run ()
{
	try
	{
		while (true)
		{
			data = in.read ();
			out.write (data);
			System.out.write (data);
		}
	} catch (IOException ioe)
	{}
}
}