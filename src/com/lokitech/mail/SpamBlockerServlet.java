package com.lokitech.mail;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import org.apache.james.*;
/**
 * Blocks spam using the MAPS RBL system.  See http://maps.vix.com/rbl/ for more information.
 */
public class SpamBlockerServlet extends MailServlet
{
	//This is the servername we postpend to IP addresses
	public final static String serverName = "rbl.maps.vix.com";
/**
 * This method was created in VisualAge.
 * @return java.util.Enumeration
 * @param server java.lang.String
 */
private boolean isSpammer (InetAddress addr) throws IOException
{
	//Construct the host name to test for spamming
	String host = "";
	byte octets[] = addr.getAddress ();
	for (int i = 3; i >= 0; i--)
		host += octets[i] + ".";
	host += serverName;
	
	//This is the query going to the server
	DNS.Message query = new DNS.Message();
	
	//We need to build a round-robin style DNS resolver
	StringTokenizer st = new StringTokenizer ("38.177.223.11", ",", false);
	while (st.hasMoreTokens ())
	{
		try
		{
			//This is the DNS server we resolve with
			DNS.Resolver res = new DNS.Resolver (st.nextToken ());

			//Not quite sure....
			query.getHeader().setFlag(DNS.dns.RD);
			query.getHeader().setOpcode(DNS.dns.QUERY);

			//This is the record that defines the query
			DNS.Record rec = DNS.Record.newRecord(new DNS.Name (host), DNS.dns.ANY, DNS.dns.ANY);
			query.addRecord(DNS.dns.QUESTION, rec);

			//Do the query!
			DNS.Message response = res.send(query);

			/*
			//Section 0 is the query, so section 1 should be the responses, section 2 is the ANY portion
			java.util.Enumeration e = response.getSection (1);
			for (;e.hasMoreElements ();)
		 	{
			 	DNS.MXRecord r = (DNS.MXRecord)e.nextElement ();
			 	System.out.println (r.getName ());
			 	System.out.println (r.getTarget ());
			 	System.out.println (r.getPriority ());
				//System.out.println (e.nextElement ());
			}
			*/
			return response.getSection (1).hasMoreElements ();
/*
			if (response.getSection (1).hasMoreElements ())
				return response.getSection (1);
			else if (response.getSection (2) != null && response.getSection (2).hasMoreElements ())
				return response.getSection (2);
			else if (response.getSection (3) != null && response.getSection (3).hasMoreElements ())
				return response.getSection (3);
//			Vector orig = new Vector ();
//			rec =  new DNS.ARecord(new DNS.Name (host), DNS.dns.ANY, 0, 0, new DNS.Name (host));
//			orig.addElement (rec);
			
//			return orig.elements ();
			return null;
*/
		} catch (Exception e)
		{
			//We failed to resolve the DNS entry (null pointer exception most likely)
			//We'll fail through, try a different DNS server...
		}
	}
	return false;
}
/**
 * Converts a String such as "uhura ([38.177.223.15])" to "38.177.223.15", etc...
 * @return java.lang.String
 * @param server java.lang.String
 */
private String parseServer (String server)
{
	server = server.trim ();
	int index = server.indexOf ('[');
	if (index > -1)
	{
		index++;
		int end = server.indexOf (']', index);
		if (end > -1)
			return server.substring (index, end).trim ();
	}
	index = server.indexOf ('(');
	if (index > -1)
		return server.substring (0, index).trim ();
	
	index = server.indexOf (' ');
	if (index > -1)
		return server.substring (0, index).trim ();

	return server;
}
/**
 * service method comment.
 */
public void service(MimeMessage message, DeliveryState state) throws IOException, MessagingException
{
	//Scan all the Received headers to look for domain names
	
	//The list of servers that need to be tested as spammers
	Vector servers = new Vector ();
	
	String headers[] = message.getHeader ("Received");
	if (headers == null)
		return;
	
	for (int i = 0; i < headers.length; i++)
	{
		//System.out.println (">>" + headers[i]);

		headers[i] = headers[i].toLowerCase ();
		
		int index = headers[i].indexOf ("from");
		if (index > -1)
		{
			index += 5;	//Move it pass the space after FROM
			int end = headers[i].indexOf ("by", index);
			if (end > -1)
			{
				String server = headers[i].substring (index, end).trim ();
				server = parseServer (server);
				servers.addElement (server);
			}
			//I'm going with the assumption that "BY" and "FROM" servers will
			//be duplicated *AND* the FROM is more reliable because the BY can
			//be spoofed by someone malicious.
		}
/*
		int index = 0;
		while ((index = headers[i].indexOf ('[', index)) > -1)
		{
			index++;
			int end = headers[i].indexOf (']');
			if (end > -1)
			{
				String server = headers[i].substring (index, end - 1);
				servers.addElement (server);
				index = end + 1;
			} else
				break;
			
		}
*/
	}

	for (int i = 0; i < servers.size (); i++)
	{
		System.out.print ("<<" + servers.elementAt (i) + " -- ");
		InetAddress addr = null;
		try
		{
			addr = InetAddress.getByName ((String)servers.elementAt (i));
		} catch (Exception e)
		{}
		if (addr == null)
		{
			System.out.println ("unresolved");
		} else
		{
			//Perform the DNS lookup on this address
			boolean spammer = isSpammer (addr);
			
			if (spammer)
				System.out.println ("SPAMMER");
			else
				System.out.println ("not spammer");

		}
	}
	
}
}