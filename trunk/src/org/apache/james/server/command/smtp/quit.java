package org.apache.james.server.command.smtp;

import org.apache.james.server.*;
import org.apache.james.*;
import org.apache.james.server.protocol.ProtocolHandler;
import org.apache.james.server.command.*;

/**
 * Command handler for the SMTP QUIT Command
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class quit
	extends GenericCommand
{

	private MailServletContext context;

	public void init(JamesServletConfig config)
	{
		context = config.getContext();
	}

	public CommandHandlerResponse service(String command, ProtocolHandler p)
	{
		if (command.trim().toUpperCase().startsWith("QUIT")) {
			CommandHandlerResponse chr = new CommandHandlerResponse(221, context.getProperty(JamesMailContext.SERVER_NAME) + " Service closing transmission channel");
			chr.setExitStatus(CommandHandlerResponse.EXIT);
			return chr;
		}
		return null;
	}
	
	public String[] getHelp(String command) {	
		String[] s = {	"QUIT",
										"    Exit " + context.getProperty(JamesMailContext.SERVER_TYPE) + "." };
		return s; 
	}
	
	public String getExtended(String command) {
		return null; 
	}

}
