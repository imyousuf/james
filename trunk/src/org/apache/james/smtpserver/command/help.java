package org.apache.james.smtpserver.command;

import org.apache.james.smtpserver.*;
import org.apache.james.MailServletContext;
import java.io.PrintWriter;
import java.util.*;
import org.apache.java.util.*;
import org.apache.avalon.blocks.*;

/**
 * Command handler for the SMTP HELP Command
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class help
	extends GenericCommand
{

	private Logger logger;
	private MailServletContext context;

	public void init(Context context)
		throws Exception
	{
		this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
	}

	public CommandHandlerResponse service(String command, ProtocolHandler p)
	{
		Hashtable state = p.getState();
		String commandgiven = null;
		String help = null;		

		// Check out the command given
		// Checkin to make sure there is something following the command
		if ( command.indexOf(" ") < 0 )
		{
			// Only HELP
			commandgiven = command.trim();
		} else {
			// Check out the command given after HELP
			commandgiven = command.trim().substring(0, command.indexOf(" ")).toUpperCase();
			help = command.substring(command.indexOf(" ")+1);
		}

		PrintWriter out = p.getOut();
		CommandHandlerResponse chr = null;
		
		if (help != null) {
		// If help is requested for a specific command

				String[] output = getCommandsHelp( p, help );

				if ( output == null ) {

					return new CommandHandlerResponse(504, "HELP topic for \"" + help + "\" unavailable");
					
				} else {

					// Add the help output to the response object				
					for(int i=0; i<java.lang.reflect.Array.getLength(output); i++) {
						if ( i == 0 ) {
							chr = new CommandHandlerResponse(214, output[i]);
						} else {
							chr.addResponseLine(output[i]);
						}
					}//for
				}
			
		} else {
		// Help was called in general
		
				// Create the response and start populating
				chr = new CommandHandlerResponse(214, "This is " + (String)state.get(p.SERVER_TYPE));
				chr.addResponseLine("Topics:");
				
				Hashtable chs = p.getAllCommandProcessors();
				
				int index = 1;
				StringBuffer output = new StringBuffer();
				
				// Creates the list of available commands
				// Should look something like
				//214-		HELO		QUIT		ETC			ETC
				
				// TODO Should parse through all commands and only output the ones
				//  Where help is available..  I think that is what sendmail does..
				
				String CommandName = null;
				String[] HelpLines = null;
				
				for (Enumeration e = chs.keys(); e.hasMoreElements();) {
					
					// next command name
					CommandName = (String) e.nextElement();
					
					// get help for this command
					HelpLines = getCommandsHelp( p, CommandName );

					// if there is no help available for this command
					if ( HelpLines == null ) {
						continue;
					}

					if (index == 4) {
						output.insert(0, "    "); // Add some space in front of command
						//System.out.println(output.toString());
						chr.addResponseLine(output.toString());						
						index = 1;
						output = new StringBuffer();
					}
						output.append( CommandName ); // Add command name
						output.append( "    " ); // Add some space between commands
						index++;
				}
				
				// Take care of last line
				output.insert(0, "    "); // Add some space in front of command
				chr.addResponseLine(output.toString());
				
				chr.addResponseLine("For more info use \"HELP <topic>\".");
				chr.addResponseLine("For local information send email to Postmaster at your site.");
		
		}
		
		chr.addResponseLine("End of HELP info");

		return chr;
	}
	
	public String[] getHelp(String command) {
		String[] s = {	command + " [ <topic> ]",
										"    The HELP command gives help info." };
		return s; 
	}
	
	public String getExtended(String command) {
		return "HELP"; 
	}
	
	public String toString() {
		return "SMTP Help Command Processor";
	}
	
	private String[] getCommandsHelp (ProtocolHandler p, String command) {
	
			// Get the command handlers for this command
			SMTPCmdHdlr ch = (SMTPCmdHdlr) p.getCommandProcessors( command );
			
			if (ch == null) {
				// Doesn't look like we support this command
				return null;
			} else {
				// Get the help output for this command handler
				return ch.getHelp( command );				
			}
		}
}
