package org.apache.james.server.command.smtp;

import org.apache.james.server.*;
import org.apache.james.server.command.*;
import org.apache.james.util.Logger;
import java.util.*;

/**
 * Manage any command handlers for SMTP commands.
 *
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class SMTPCommandManager
	extends CommandManager implements SMTPCmdHdlr
{

	Logger logger;

	public SMTPCommandManager(Properties props, JamesServ js)
		throws Exception
	{
		super(props, js);
		logger = (Logger) js.getLogger();
	}

		/**
		 * Handles HELP requests for the command handlers
		 * @param command java.lang.String
		 * @return java.lang.String[]
		 */
    public String[] getHelp(String command) {
			String response[] = null;
			if ( chSingle != null) {
				// get help for one command handler
				response = ((SMTPCmdHdlr)chSingle).getHelp(command);
			} else {
				// get help for each command handler in the group
				for (Enumeration chs = chGroup.elements() ; chs.hasMoreElements() ;) {
      		response = ((SMTPCmdHdlr) chs.nextElement()).getHelp(command);
					// return the first valid help response
					if (response != null) {
						break;
					}
				}
			}
			return response;
		}

		/**
		 * Handles EHLO requests for the command handlers
		 * @param command java.lang.String
		 * @return java.lang.String[]
		 */
    public String getExtended(String command) {
			String response = null;
			if ( chSingle != null) {
				// get ehlo for one command handler
				response = ((SMTPCmdHdlr)chSingle).getExtended(command);
			} else {
				// get ehlo for each command handler in the group
				for (Enumeration chs = chGroup.elements() ; chs.hasMoreElements() ;) {
      		response = ((SMTPCmdHdlr) chs.nextElement()).getExtended(command);
					// return the first valid ehlo response
					if (response != null) {
						break;
					}
				}
			}
			return response;
		}

}

