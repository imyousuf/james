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
 * A very simple servlet, this performs what mail servers generally call "aliasing" where
 * one address is mapped to another.  Right now it does not support any wildcards.  Aliases
 * are defined in a configuration file, with 2 email addresses per line, the original and
 * the address to map to.  For example:
 <PRE>
sergek@lokitech.com  sknystautas@lokitech.com
serge@lokitech.com   sknystautas@lokitech.com
<jon.stevens@lokitech.com>  <jon@clearink.com>
</PRE>
  Addresses can optionally contain <>, but should use only whitespace to delimit the two
  addresses.  This text configuration file should be specified in the servlet's confFile setting.
  For example:
  <PRE>
processor.12.code=com.lokitech.mail.AliasesServlet
processor.12.confFile=c:/Java/James/aliases.conf
</PRE>
 * @author Serge Knystautas <a href="mailto:sergek@lokitech.com">sergek@lokitech.com</a>
 * @version 1.0
 */
public class AliasesServlet extends MailServlet
{
	Hashtable aliases = null;			//This contains the paired address/new address combinations
/**
 * This method was created in VisualAge.
 * @param config org.apache.james.MailServletConfig
 */
public void init (MailServletConfig config) throws MessagingException
{
	super.init (config);

	try
	{
		File file = config.getConfFile ();
		aliases = new Hashtable ();
		
		if (file != null && file.exists ())
		{
			BufferedReader reader = new BufferedReader (new FileReader (file));
			String line = null;
			int count = 0;
			while ((line = reader.readLine ()) != null)
			{
				count++;
				InternetAddress addr[] = InternetAddress.parse (line);
				if (addr.length != 2)
				{
					System.out.println ("line " + count + " does not a proper pair of addresses");
					continue;
				}
				aliases.put (addr[0], addr[1]);
			}
		}
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
		if (aliases.get (addr[i]) != null)
			addr[i] = (InternetAddress)aliases.get (addr[i]);
	}
	
	//Not sure if this is really necessary, but just to be sure
	state.setRecipients (addr);
}
}