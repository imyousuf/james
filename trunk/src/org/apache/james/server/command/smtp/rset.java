package org.apache.james.server.command.smtp;

import org.apache.james.server.*;
import org.apache.james.*;
import org.apache.james.server.protocol.ProtocolHandler;
import org.apache.james.server.command.*;

/**
 * Command handler for the SMTP RSET Command
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class rset
	extends GenericCommand
{

	public CommandHandlerResponse service(String command, ProtocolHandler p)
	{
		p.resetState();
		return  new CommandHandlerResponse(250, "Reset state");
	}
	
	public String[] getHelp(String command) {	
		String[] s = {	"RSET", "    Resets the system." };
		return s; 
	}
	
	public String getExtended(String command) {
		return null; 
	}
}
