package org.apache.james.smtpserver.command;

import org.apache.james.*;
import org.apache.james.smtpserver.*;
import org.apache.james.util.Utils;
import java.lang.reflect.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.util.*;
import org.apache.avalon.util.*;
import org.apache.avalon.SimpleContext;

import org.apache.avalon.blocks.*;
import org.apache.java.util.*;

/**
 * Class to manage any number of classes that handle one command.
 *
 * @author Matthew Petteys <matt@arcticmail.com>
 */

public class CommandManager	implements CommandHandler
{

    private Logger logger;
    private Configuration conf;

    // Holds the command handlers for this command manager
    Vector chGroup;
    CommandHandler chSingle;

    /**
     * Creates the command manager
     * @param context org.apache.java.util.Context
     */
    public CommandManager(Context context) throws Exception {	
    
        this.conf = context.getConfiguration();
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
        
        chGroup = null;
        chSingle = null;
        
        Enumeration commandsE;
        for (commandsE = conf.getChildren("class"); commandsE.hasMoreElements();) {

            CommandHandler handler = null;
            Configuration node = (Configuration) commandsE.nextElement();

            String className = node.getAttribute("name");
            System.out.println("Initializing command processor for " + className);

            // Create the command handler
            try {
                handler = (CommandHandler) Class.forName( className ).newInstance();
            } catch (Exception e) {
                logger.log("instantiateCommandHandler : Class.forName Exception " + e.getMessage(), logger.ERROR);
                throw e;
            }

            // give command handlers access to a needed objects
            SimpleContext sc = new SimpleContext(node);
            sc.put(Interfaces.LOGGER, logger);
            sc.put("spool", (MessageSpool) context.getImplementation("spool") );
            sc.put("messageid", (SMTPServer.MessageId) context.getImplementation("messageid") );

            // Initialize it
            try {
                handler.init( sc );
            } catch (Exception e) {
                logger.log("initializeCommandHandler : init Exception for " + className + " : " + e.getMessage(), logger.ERROR);
                throw e;
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
        }	
    }

    /**
     * Empty init method from CommandHandler interface so this object 
     *  can be referenced as a CommandHandler
     * @param config org.apache.james.server.JamesServletConfig
     * @return void
   */
    public void init(Context config) throws Exception {}
    
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

