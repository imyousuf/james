package org.apache.james.server.command;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*; // Required for javax.mail
import org.apache.james.server.*;
import org.apache.james.*;
import org.apache.james.server.protocol.ProtocolHandler;

/**
 * This is the command handler for commands
 * @author Matthew Petteys <matt@arcticmail.com>
 * @version 0.1
 */

public interface CommandHandler
{

    /**
     * Initialize the command handler
     * @param config org.apache.james.server.JamesServletConfig
		 * @return void
     * @see JamesServletConfig
     */
		public void init(JamesServletConfig config)
			throws Exception;

    /**
		 * Logs an error message
     * @param message java.lang.String
		 * @return void
		 */
		public void log(String message);

    /**
		 * Processes a command given on the socket.  Returns a response object that will be
		 *  returned to the client.
		 * - CommandHandler should return null if it cannont provide an appropriate response
     * @param command java.lang.String
     * @param p org.apache.james.ProtocolHandler
     * @exception java.io.Exception
     * @return org.apache.server.CommandHandlerResponse
     */
    public abstract CommandHandlerResponse service(String command, ProtocolHandler p)
			throws Exception;

    /**
     * Destorys the command processor
		 * @return void
     */
    public abstract void destroy();
		
    /**
     * Returns a string describing this command processor and state
		 * @return java.lang.String
     */
		public String toString();

}
