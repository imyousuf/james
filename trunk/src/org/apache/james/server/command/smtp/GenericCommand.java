package org.apache.james.server.command.smtp;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*; // Required for javax.mail
import org.apache.james.server.*;
import org.apache.james.MailServletContext;
import org.apache.james.server.protocol.ProtocolHandler;

/**
 * This is a generic command handler for SMTP commands
 * @author Matthew Petteys <matt@arcticmail.com>
 * @version 0.1
 */

public abstract class GenericCommand
	implements SMTPCmdHdlr
{

		MailServletContext context = null;
		
    /**
     * Initialize the mail servlet
     * @param config MailServletConfig
		 * @return void
     */
    public void init(JamesServletConfig config)
			throws Exception
		{
        context = config.getContext();
        log("Init CH:" + this.toString());
    }

    /**
     * Logs to the JamesServletConfig
     * @return org.apache.james.server.MailServletContext 
     */
		public MailServletContext getContext()
		{
			return context;
		}

    /**
     * Logs to the JamesServletConfig
     * @param message java.lang.String
		 * @return void
     */
    public void log(String message) {
			if (context != null) {
        getContext().log(getClass().getName() + ": " + message);
			}
    }

    /**
     * Logs to the JamesServletConfig
     * @param message java.lang.String
		 * @return void
     */
		public void destroy() {
			log("Destroy CH:" + this.toString());
		}
		
}
