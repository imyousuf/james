package org.apache.james.server.command;

import org.apache.james.*;
import org.apache.james.server.*;
import org.apache.james.server.protocol.ProtocolHandler;
import org.apache.james.util.Utils;
import org.apache.james.util.Logger;
import java.lang.reflect.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.util.*;

/**
 * Class to manage any number of classes that handle one command.
 *
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class CommandManager
	implements CommandHandler
{

	Logger logger;
	JamesServ server;
	public Vector chGroup;
	public CommandHandler chSingle;

	/**
	 * Creates the command manager
	 * @param props java.util.Properties
	 * @param server org.apache.james.server.JamesServ
	 */
	public CommandManager(Properties props, JamesServ server)
		throws Exception
	{
		
		logger = (Logger) server.getLogger();
		
		chGroup = null;
		chSingle = null;

		int index = 1;
		String CurrIndex = String.valueOf(index);
		String ClsName = props.getProperty(CurrIndex);
		String servletArgs = null;
		String servletConfFile = null;
		Properties servletProps = null;
		JamesServletConfig servletConfig = null;
		CommandHandler handler;
		
		while ( ClsName != null ) {

			// Create the command handler
			handler = (CommandHandler) Class.forName( ClsName ).newInstance();
			
			servletArgs = props.getProperty(CurrIndex + ".initArgs");
      servletConfFile = props.getProperty(CurrIndex + ".confFile");
      servletProps = Utils.parseArgs(servletArgs);
      servletConfig = new JamesServletConfig(servletProps, servletConfFile, new JamesMailContext(server));
					
			// Initialize it
			try {
				handler.init( servletConfig );
			} catch (Exception e) {
				throw new Exception("Error initializing command handler:" + ClsName);
			}

			if (chSingle == null && chGroup == null) {
				chSingle = handler;
			} else if (chGroup == null) {
				chGroup = new Vector(2);
				chGroup.addElement(chSingle);
				chGroup.addElement(handler);
				chSingle = null;			
			} else {
				chGroup.addElement(handler);
			}

			// Get class name for next command handler
			ClsName = props.getProperty(String.valueOf(++index));	
		}
		
		if (index == 1) {
			throw new Exception("No command handlers found");
		}	
	}

	/**
	 * Empty init method from CommandHandler interface so this object 
	 *  can be referenced as a CommandHandler
	 * @param config org.apache.james.server.JamesServletConfig
	 * @return void
   */
	public void init(JamesServletConfig config) throws Exception {}
	
	/**
	 * Log method from CommandHandler interface so this object can be 
	 *  referenced as a CommandHandler
	 * @param message java.lang.String
	 * @return void
   */
	public void log(String message) { logger.log(message); }

	/**
	 * Service method from CommandHandler interface
	 *  Manages the responses from all the command handlers for this command.
	 * @param commandLine java.lang.String
	 * @param p org.apache.james.server.protocol.ProtocolHandler
	 * @return org.apache.james.server.protocol.CommandHandlerResponse
   */
  public CommandHandlerResponse service(String commandLine, ProtocolHandler p)
			throws Exception
	{
	
		logger.log("Entering command manager service:" + commandLine);

		CommandHandlerResponse chr = null;

		if ( chSingle != null) {
			chr = chSingle.service(commandLine, p);
		} else {
		
			CH: for (Enumeration chs = chGroup.elements() ; chs.hasMoreElements() ;) {
      	CommandHandler handler = (CommandHandler) chs.nextElement();
				try {
				
					chr = handler.service(commandLine, p);
											
					if (chr != null && ( chr.getExitStatus() == CommandHandlerResponse.OK || chr.getExitStatus() == CommandHandlerResponse.EXIT )) {
						break CH;
					}
											
				} catch (Exception e) {		
						chr = new CommandHandlerResponse (500, "Error Processing Command");
						logger.log("Error in service of command handler:" + commandLine);
				}											
     	}
		}
		return chr;
	}

	/**
	 * Cleans up the this object and all the command handler objects handled
	 *  by this manager
	 * @return void
	 */
	public void destroy()
	{
		if ( chSingle != null) {
			chSingle.destroy();
		} else {
			for (Enumeration chs = chGroup.elements() ; chs.hasMoreElements() ;) {
      	((CommandHandler) chs.nextElement()).destroy();
			}
		}
	}
}

