package org.apache.james.server.command.smtp;

import org.apache.james.server.*;
import org.apache.james.*;
import org.apache.james.server.protocol.ProtocolHandler;
import org.apache.james.server.command.*;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Command handler to check the sending host for RBL/ORBS Relay
 *
 * @author Mattehw Petteys <matt@arcticmail.com>
 */

public class rbl
	extends GenericCommand
{

	/**
	 * Init string checked for the search domain.
	 *  Defaults to ".rbl.maps.vix.com"
	 */
	private final String domain = "domain";

	/**
	 * Init string checked for the SMTP response code that will be replied.
	 *  Defaults to "551"
	 */
	private final String response = "code";

	/**
	 * Init string checked for the text of response.
	 *  Defaults to "Mail not allowed from your host"
	 */
	private final String text = "text";

	String searchDomain;
	int rcode = 550;
	String rdesc = "Mail not allowed from your host";

	public void init(JamesServletConfig config)
		throws Exception
	{
	
		// Check to see if another response code is specified
		if ( config.getInitParameter(response) != null ) {
			try {
				rcode = new Integer(config.getInitParameter(response)).intValue();
			} catch (NumberFormatException nfe) {
				throw new Exception("Invalid response code specified");
			}
		}
		
		// Check to see if another response text is specified
		if ( config.getInitParameter(text) != null ) {
			rdesc = config.getInitParameter(text);
		}
		
		// Check to see if a search domain is specified		
		if ( config.getInitParameter(domain) == null ) {
				throw new Exception("No search domain specified");
		}
		searchDomain = config.getInitParameter(domain);
		
	}

	public CommandHandlerResponse service(String command, ProtocolHandler p)
	{
	
		Hashtable state = p.getState();

		StringBuffer lookup_name = new StringBuffer();
		
		//StringTokenizer host = new StringTokenizer("127.0.0.2", "."); // Test Host from RBL
		StringTokenizer host = new StringTokenizer((String) state.get(ProtocolHandler.REMOTE_IP), ".");

		// Reverse the hostname into below format
		// 2.0.0.127.rbl.maps.vix.com
		while ( host.hasMoreTokens() ) {
		
			// prepend the pieces of the ipaddress as I read them from tokenizer
			lookup_name.insert(0, host.nextElement());
			
			if ( host.hasMoreTokens() ) {  // don't want to append . to front of hostname
				lookup_name.insert(0, ".");
			}
		}

		lookup_name.append( searchDomain );		

		try {
		System.out.println(lookup_name.toString());
			java.net.InetAddress.getByName(lookup_name.toString());
			return new CommandHandlerResponse(rcode, rdesc, CommandHandlerResponse.DONE );
		} catch (java.net.UnknownHostException uke) {}
		return null;
	}
	
	public String[] getHelp(String command) {
		return null;
	}
	
	public String getExtended(String command) {
		return null; 
	}

}
