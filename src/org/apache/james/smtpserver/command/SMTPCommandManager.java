package org.apache.james.smtpserver.command;

import org.apache.james.smtpserver.*;
import java.util.*;

import org.apache.avalon.blocks.*;
import org.apache.java.util.*;

/**
 * Manage any command handlers for SMTP commands.
 *
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class SMTPCommandManager
    extends org.apache.james.smtpserver.command.CommandManager implements SMTPCmdHdlr
{

    Logger logger;

    public SMTPCommandManager(Context context)
        throws Exception
    {
        super( context );
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
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

