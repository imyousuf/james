package org.apache.james.smtpserver.command;

import org.apache.james.smtpserver.*;
import org.apache.james.*;
import java.io.*;
import java.util.*;
import org.apache.java.util.*;
import org.apache.avalon.blocks.*;

/**
 * Command handler for the SMTP HELO/EHLO Command
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class helo
	extends GenericCommand
{

  private Logger logger;
	private static String CURRENT_HELO_MODE = "HELO_MODE";

	public void init(Context context)
		throws Exception
	{
		this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
	}
	
	public CommandHandlerResponse service(String command, ProtocolHandler p)
	{
		Hashtable state = p.getState();
		String commandgiven = null;
		String hostgiven = null;

		if (state.containsKey(CURRENT_HELO_MODE)) {
			return new CommandHandlerResponse(250,  (String) state.get(p.SERVER_NAME) + " Duplicate HELO/EHLO");
		}

		if ( command.indexOf(" ") < 0 )
		{ // Checkin to make sure there is something following the command
			commandgiven = command.trim();
		} else {
			commandgiven = command.trim().substring(0, command.indexOf(" ")).toUpperCase();
			hostgiven = command.substring(command.indexOf(" ")+1);
		}
    
		//context.log("helo command proc: " + commandgiven + ":" + hostgiven);

		PrintWriter out = p.getOut();
		
		if ( hostgiven == null )
		{
			return new CommandHandlerResponse(501, commandgiven + " requires domain address");
		}

		CommandHandlerResponse ch = null;

		if ( commandgiven.equals("HELO") )
		{
			logger.log("helo start");

			state.put(ProtocolHandler.NAME_GIVEN, hostgiven);
			state.put(CURRENT_HELO_MODE, "HELO");
			
			p.setState(state);
			
			ch = new CommandHandlerResponse(250, (String) state.get(p.SERVER_NAME) + " Hello " + hostgiven + " (" + (String)state.get(p.REMOTE_NAME) + " [" + (String)state.get(p.REMOTE_IP) + "])", CommandHandlerResponse.OK);

		} else
		if ( commandgiven.equals("EHLO") )
		{
			logger.log("ehlo start");
		
			state.put(ProtocolHandler.NAME_GIVEN, hostgiven);
			state.put(CURRENT_HELO_MODE, "EHLO");

			p.setState(state);

			ch = new CommandHandlerResponse(250, (String) state.get(p.SERVER_NAME) + " Hello " + hostgiven + " (" + (String)state.get(p.REMOTE_NAME) + " [" + (String)state.get(p.REMOTE_IP) + "])", CommandHandlerResponse.OK);

			Hashtable allHandlers = p.getAllCommandProcessors();
			String output = null;

			for (Enumeration e = allHandlers.keys(); e.hasMoreElements();) {
			
				output = ((SMTPCmdHdlr) allHandlers.get(e.nextElement())).getExtended( commandgiven );
			
				if ( output != null ) {
					ch.addResponseLine(output);
					output = null;					
				}
			}			
		}
		return ch;
	}

	public String[] getHelp(String command)
	{	
		if ( command.equals("HELO") ) {
			String[] s = {	command + " <hostname>",
											"    Introduce yourself." };
			return s;
		}
		return null;
	}
	
	public String getExtended(String command) {
		return null; 
	}
	
	public String toString() {
		return "Basic HELO/EHLO command processor";
	}

}

