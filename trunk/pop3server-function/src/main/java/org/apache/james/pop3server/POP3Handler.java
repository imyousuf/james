/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/



package org.apache.james.pop3server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.commons.logging.Log;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.james.socket.CRLFTerminatedReader;
import org.apache.james.socket.ProtocolContext;
import org.apache.james.socket.ProtocolHandler;
import org.apache.james.socket.Watchdog;
import org.apache.mailet.Mail;

/**
 * The handler class for POP3 connections.
 *
 */
public class POP3Handler implements POP3Session, ProtocolHandler {

    private ProtocolContext context;
    
    private final static byte COMMAND_MODE = 1;
    private final static byte RESPONSE_MODE = 2;

   

    // Authentication states for the POP3 interaction
    /** Waiting for user id */
    public final static int AUTHENTICATION_READY = 0;
    /** User id provided, waiting for password */
    public final static int AUTHENTICATION_USERSET = 1;  
    /**
     * A valid user id/password combination has been provided.
     * In this state the client can access the mailbox
     * of the specified user.
     */
    public final static int TRANSACTION = 2;              

    /**
     * A placeholder for emails deleted during the course of the POP3 transaction.  
     * This Mail instance is used to enable fast checks as to whether an email has been
     * deleted from the inbox.
     */
    public final static Mail DELETED = new MailImpl();    

    /**
     * The per-service configuration data that applies to all handlers
     */
    private final POP3HandlerConfigurationData theConfigData;

    /**
     * The mail server's copy of the user's inbox
     */
    private MailRepository userInbox;

    /**
     * The current transaction state of the handler
     */
    private int handlerState;

    /**
     * A dynamic list representing the set of
     * emails in the user's inbox at any given time
     * during the POP3 transaction.
     */
    private List<Mail> userMailbox = new ArrayList<Mail>();

    /**
     * A snapshot list representing the set of
     * emails in the user's inbox at the beginning
     * of the transaction
     */
    private List<Mail> backupUserMailbox;  

    /**
     * The per-handler response buffer used to marshal responses.
     */
    private StringBuilder responseBuffer = new StringBuilder(256);


    /**
     * The POP3HandlerChain object set by POP3Server
     */
    private final POP3HandlerChain handlerChain;

    /**
     * The session termination status
     */
    private boolean sessionEnded = false;


    /**
     * The user name of the authenticated user associated with this POP3 transaction.
     */
    private String authenticatedUser;

    /**
     * The mode of the current session
     */
    private byte mode;
    
    /**
     * If not null every line is sent to this command handler instead
     * of the default "command parsing -> dipatching" procedure.
     */
    private LinkedList<LineHandler> lineHandlers;

    /**
     * Connect Handlers
     */
    private final LinkedList<ConnectHandler> connectHandlers;
    
    public POP3Handler(final POP3HandlerConfigurationData theConfigData, final POP3HandlerChain handlerChain) {
        this.theConfigData = theConfigData;
        this.handlerChain = handlerChain;
        connectHandlers = handlerChain.getHandlers(ConnectHandler.class);
        lineHandlers = handlerChain.getHandlers(LineHandler.class);
    }
    
    /**
     * @see org.apache.james.socket.AbstractJamesHandler#handleProtocol(ProtocolContext)
     */
    public void handleProtocol(ProtocolContext context) throws IOException {
        this.context = context;
        handlerState = AUTHENTICATION_READY;
        authenticatedUser = "unknown";

        sessionEnded = false;


        //Session started - RUN all connect handlers
        if(connectHandlers != null) {
            int count = connectHandlers.size();
            for(int i = 0; i < count; i++) {
                connectHandlers.get(i).onConnect(this);
                if(sessionEnded) {
                    break;
                }
            }
        }

        context.getWatchdog().start();
        while(!sessionEnded) {
          //parse the command
          String line =  null;
         
          line = readCommandLine();
        
          if (line == null) {
              break;
          }

          if (lineHandlers.size() > 0) {
              ((LineHandler) lineHandlers.getLast()).onLine(this, line);
          } else {
              sessionEnded = true;
          }
          context.getWatchdog().reset();
          
        }
        context.getWatchdog().stop();
        if (context.getLogger().isInfoEnabled()) {
            StringBuilder logBuffer =
                new StringBuilder(128)
                    .append("Connection for ")
                    .append(getUser())
                    .append(" from ")
                    .append(context.getRemoteHost())
                    .append(" (")
                    .append(context.getRemoteIP())
                    .append(") closed.");
            context.getLogger().info(logBuffer.toString());
        }
       
       
    }
    
    /**
     * @see org.apache.james.socket.AbstractJamesHandler#fatalFailure(java.lang.RuntimeException, ProtocolContext)
     */
    public void fatalFailure(RuntimeException e, ProtocolContext context) {
        try {
            context.getOutputWriter().println(POP3Response.ERR_RESPONSE + " Error closing connection.");
            context.getOutputWriter().flush();
        } catch (Throwable t) {
            
        }
    }

    /**
     * Resets the handler data to a basic state.
     */
    public void resetHandler() {
        // Clear user data
        authenticatedUser = null;
        userInbox = null;
        if (userMailbox != null) {
            Iterator<Mail> i = userMailbox.iterator();
            while (i.hasNext()) {
                ContainerUtil.dispose(i.next());
            }
            userMailbox.clear();
            userMailbox = null;
        }

        if (backupUserMailbox != null) {
            Iterator<Mail> i = backupUserMailbox.iterator();
            while (i.hasNext()) {
                ContainerUtil.dispose(i.next());
            }
            backupUserMailbox.clear();
            backupUserMailbox = null;
        }
        
        // empty any previous line handler and add self (command dispatcher)
        // as the default.
        lineHandlers = handlerChain.getHandlers(LineHandler.class);
    }

    /**
     * Reads a line of characters off the command line.
     *
     * @return the trimmed input line
     * @throws IOException if an exception is generated reading in the input characters
     */
    private String readCommandLine() throws IOException {
        for (;;) try {
            String commandLine = context.getInputReader().readLine();
            if (commandLine != null) {
                commandLine = commandLine.trim();
            }
            return commandLine;
        } catch (CRLFTerminatedReader.TerminationException te) {
            context.writeLoggedFlushedResponse("-ERR Syntax error at character position " + te.position() + ". CR and LF must be CRLF paired.  See RFC 1939 #3.");
        }
    }

    /**
     * This method parses POP3 commands read off the wire in handleConnection.
     * Actual processing of the command (possibly including additional back and
     * forth communication with the client) is delegated to one of a number of
     * command specific handler methods.  The primary purpose of this method is
     * to parse the raw command string to determine exactly which handler should
     * be called.  It returns true if expecting additional commands, false otherwise.
     */
    
    /**
     * @see org.apache.james.pop3server.POP3Session#getRemoteHost()
     */
    public String getRemoteHost() {
        return context.getRemoteHost();
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return context.getRemoteIP();
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#endSession()
     */
    public void endSession() {
        sessionEnded = true;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#isSessionEnded()
     */
    public boolean isSessionEnded() {
        return sessionEnded;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getUser()
     */
    public String getUser() {
        return authenticatedUser;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#setUser(java.lang.String)
     */
    public void setUser(String userID) {
        authenticatedUser = userID;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#clearResponseBuffer()
     */
    public String clearResponseBuffer() {
        String responseString = responseBuffer.toString();
        responseBuffer.delete(0,responseBuffer.length());
        return responseString;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getWatchdog()
     */
    public Watchdog getWatchdog() {
        return context.getWatchdog();
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#writeResponse(java.lang.String)
     */
    public void writeResponse(String respString) {
        context.writeLoggedFlushedResponse(respString);
        //TODO Explain this well
        if(mode == COMMAND_MODE) {
            mode = RESPONSE_MODE;
        }
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getConfigurationData()
     */
    public POP3HandlerConfigurationData getConfigurationData() {
        return theConfigData;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getHandlerState()
     */
    public int getHandlerState() {
        return handlerState;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#setHandlerState(int)
     */
    public void setHandlerState(int handlerState) {
        this.handlerState = handlerState;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getUserInbox()
     */
    public MailRepository getUserInbox() {
        return userInbox;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#setUserInbox(org.apache.james.services.MailRepository)
     */
    public void setUserInbox(MailRepository userInbox) {
        this.userInbox = userInbox;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getUserMailbox()
     */
    public List<Mail> getUserMailbox() {
        return userMailbox;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#setUserMailbox(List)
     */
    public void setUserMailbox(List<Mail> userMailbox) {
        this.userMailbox = userMailbox;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getBackupUserMailbox()
     */
    public List<Mail> getBackupUserMailbox() {
        return backupUserMailbox;
    }


    /**
     * @see org.apache.james.pop3server.POP3Session#setUserMailbox(List)
     */
    public void setBackupUserMailbox(List<Mail> backupUserMailbox) {
        this.backupUserMailbox = backupUserMailbox;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getOutputStream()
     */
    public OutputStream getOutputStream() {
        return context.getOutputStream();
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getLogger()
     */
    public Log getLogger() {
        return context.getLogger();
    }

    /**
     * @see org.apache.james.socket.TLSSupportedSession#startTLS()
     */
	public void startTLS() throws IOException {
		context.secure();
	}

	/**
	 * @see org.apache.james.socket.TLSSupportedSession#isStartTLSSupported()
	 */
	public boolean isStartTLSSupported() {
		return getConfigurationData().isStartTLSSupported();
	}

	/**
	 * @see org.apache.james.socket.TLSSupportedSession#isTLSStarted()
	 */
	public boolean isTLSStarted() {
		return context.isSecure();
	}

	/**
	 * @see org.apache.james.pop3server.POP3Session#writePOP3Response(org.apache.james.pop3server.POP3Response)
	 */
    public void writePOP3Response(POP3Response response) {
        // Write a single-line or multiline response
        if (response != null) {
            if (response.getRawLine() != null) {
                context.writeLoggedFlushedResponse(response.getRawLine());
            } 
            
            List<CharSequence> responseList = response.getLines();
            if (responseList != null) {
                for (int k = 0; k < responseList.size(); k++) {
                    final CharSequence line = responseList.get(k);
                    context.writeLoggedFlushedResponse(line.toString());
                }
            }
           
            if (response.isEndSession()) {
                sessionEnded = true;
            }
        }
    }
	
}
