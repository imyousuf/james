package org.apache.james.server.command.smtp;

import org.apache.james.server.command.CommandHandler;

/**
 * This extends the command handler interface with functions that are
 *  more specific for STMP commands
 * @author Matthew Petteys <matt@arcticmail.com>
 * @version 0.1
 */

public interface SMTPCmdHdlr
	extends CommandHandler
{
		
		/**
     * Method involked upon the execution of SMTP HELP command for this command
		 * - should return null if it cannont provide an appropriate help text			
     * @param command java.lang.String
		 * @return boolean
     */
    public abstract String[] getHelp(String command);

		/**
     * Method involked upon the execution of SMTP EHLO command for this command
		 * - should return null if it cannont provide an appropriate EHLO text or isn't a extended command
     * @param command java.lang.String
     * @return java.lang.String[]
     */
    public abstract String getExtended(String command);

}
