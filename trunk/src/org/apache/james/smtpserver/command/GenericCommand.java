package org.apache.james.smtpserver.command;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*; // Required for javax.mail
import org.apache.james.smtpserver.ProtocolHandler;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;

/**
 * This is a generic command handler for SMTP commands
 * @author Matthew Petteys <matt@arcticmail.com>
 * @version 0.1
 */

public abstract class GenericCommand implements SMTPCmdHdlr {

    Context genericContext;
    Logger genericLogger;

    /**
    * Initialize the mail servlet
    * @param config MailServletConfig
    * @return void
    */
    public void init(Context context) throws Exception {
        this.genericContext = context;
        this.genericLogger = (Logger) genericContext.getImplementation(Interfaces.LOGGER);
        genericLogger.log("CH Init");
    }

    /**
    * Returns the context
    * @return org.apache.java.util.Context
    */
    public Context getContext() {
        return this.genericContext;
    }

    /**
    * Logs to the JamesServletConfig
    * @param message java.lang.String
    * @return void
    */
    public void log(String message) {
        if (genericLogger != null) {
            genericLogger.log(getClass().getName() + ": " + message);
        }
    }

    /**
    * Logs to the JamesServletConfig
    * @param message java.lang.String
    * @return void
    */
    public void destroy() {
        log("CH Destroy");
    }	
}
