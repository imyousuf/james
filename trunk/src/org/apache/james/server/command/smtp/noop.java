package org.apache.james.server.command.smtp;

import org.apache.james.server.*;
import org.apache.james.*;
import org.apache.james.server.protocol.ProtocolHandler;
import org.apache.james.server.command.*;

/**
 * Command handler for the SMTP NOOP Command
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class noop
	extends GenericCommand
{

	public CommandHandlerResponse service(String command, ProtocolHandler p)
	{
		return  new CommandHandlerResponse(250, "OK");
	}
	
	public String[] getHelp(String command) {	
		String[] s = {	"NOOP", "    Do nothing." };
		return s; 
	}

	public String getExtended(String command) {
		return null; 
	}
	
}
