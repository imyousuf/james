package org.apache.james.server.command.smtp;

import org.apache.james.server.*;
import org.apache.james.*;
import org.apache.james.server.protocol.ProtocolHandler;
import org.apache.james.server.command.*;

/**
 * Command handler for the SMTP SIZE Command
 *
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class size
	extends GenericCommand
{

	public String MaxSizeName = "maxsize";
	private Integer MaxSize;

	public void init(JamesServletConfig config)
		throws Exception
	{
	
		// Check to see if another response code is specified
		if ( config.getInitParameter(MaxSizeName) != null ) {
			try {
				MaxSize = new Integer(config.getInitParameter(MaxSizeName));
			} catch (NumberFormatException nfe) {
				throw new Exception("Invalid maximum message specified");
			}
		}

		// If the size wasn't specified - RFC says that size of 0 is unlimited
		if ( MaxSize == null ) {
			MaxSize = new Integer(0);
		}

	}

	public CommandHandlerResponse service(String command, ProtocolHandler p)
	{
		return null; // No command specified with SIZE
	}
	
	public String[] getHelp(String command) {	
		return null; 
	}
	
	public String getExtended(String command) {
		return "SIZE " + MaxSize;
	}
}
