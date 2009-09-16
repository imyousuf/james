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

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.Constants;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.james.socket.CRLFTerminatedReader;
import org.apache.james.socket.ProtocolHandler;
import org.apache.james.socket.ProtocolHandlerHelper;
import org.apache.james.socket.Watchdog;
import org.apache.mailet.Mail;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * The handler class for POP3 connections.
 *
 */
public class POP3Handler implements POP3Session, ProtocolHandler {

    private ProtocolHandlerHelper helper;
    
    private final static byte COMMAND_MODE = 1;
    private final static byte RESPONSE_MODE = 2;

    // POP3 Server identification string used in POP3 headers
    private static final String softwaretype        = "JAMES POP3 Server "
                                                        + Constants.SOFTWARE_VERSION;

    // POP3 response prefixes
    final static String OK_RESPONSE = "+OK";    // OK response.  Requested content
                                                        // will follow

    final static String ERR_RESPONSE = "-ERR";  // Error response.  Requested content
                                                        // will not be provided.  This prefix
                                                        // is followed by a more detailed
                                                        // error message

    // Authentication states for the POP3 interaction

    final static int AUTHENTICATION_READY = 0;    // Waiting for user id

    final static int AUTHENTICATION_USERSET = 1;  // User id provided, waiting for
                                                          // password

    final static int TRANSACTION = 2;             // A valid user id/password combination
                                                          // has been provided.  In this state
                                                          // the client can access the mailbox
                                                          // of the specified user

    static final Mail DELETED = new MailImpl();   // A placeholder for emails deleted
                                                          // during the course of the POP3
                                                          // transaction.  This Mail instance
                                                          // is used to enable fast checks as
                                                          // to whether an email has been
                                                          // deleted from the inbox.

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
    private List userMailbox = new ArrayList();

    private List backupUserMailbox;         // A snapshot list representing the set of
                                                 // emails in the user's inbox at the beginning
                                                 // of the transaction

    /**
     * The per-handler response buffer used to marshal responses.
     */
    private StringBuffer responseBuffer = new StringBuffer(256);


    /**
     * The name of the currently parsed command
     */
    String curCommandName =  null;

    /**
     * The value of the currently parsed command
     */
    String curCommandArgument =  null;

    /**
     * The POP3HandlerChain object set by POP3Server
     */
    final POP3HandlerChain handlerChain;

    /**
     * The session termination status
     */
    private boolean sessionEnded = false;


    /**
     * The hash map that holds variables for the POP3 message transfer in progress.
     *
     * This hash map should only be used to store variable set in a particular
     * set of sequential MAIL-RCPT-DATA commands, as described in RFC 2821.  Per
     * connection values should be stored as member variables in this class.
     */
    private HashMap state = new HashMap();

    /**
     * The user name of the authenticated user associated with this POP3 transaction.
     */
    private String authenticatedUser;

    /**
     * The mode of the current session
     */
    private byte mode;
    
    
    public POP3Handler(final POP3HandlerConfigurationData theConfigData, final POP3HandlerChain handlerChain) {
        this.theConfigData = theConfigData;
        this.handlerChain = handlerChain;
    }
    
    /**
     * @see org.apache.james.socket.AbstractJamesHandler#handleProtocol()
     */
    public void handleProtocol() throws IOException {
        handlerState = AUTHENTICATION_READY;
        authenticatedUser = "unknown";

        sessionEnded = false;
        resetState();

        // Initially greet the connector
        // Format is:  Sat, 24 Jan 1998 13:16:09 -0500
        responseBuffer.append(OK_RESPONSE)
                    .append(" ")
                    .append(theConfigData.getHelloName())
                    .append(" POP3 server (")
                    .append(POP3Handler.softwaretype)
                    .append(") ready ");
        String responseString = clearResponseBuffer();
        helper.writeLoggedFlushedResponse(responseString);

        //Session started - RUN all connect handlers
        List connectHandlers = handlerChain.getConnectHandlers();
        if(connectHandlers != null) {
            int count = connectHandlers.size();
            for(int i = 0; i < count; i++) {
                ((ConnectHandler)connectHandlers.get(i)).onConnect(this);
                if(sessionEnded) {
                    break;
                }
            }
        }

        
        helper.getWatchdog().start();
        while(!sessionEnded) {
          //Reset the current command values
          curCommandName = null;
          curCommandArgument = null;
          mode = COMMAND_MODE;

          //parse the command
          String cmdString =  readCommandLine();
          if (cmdString == null) {
              break;
          }
          int spaceIndex = cmdString.indexOf(" ");
          if (spaceIndex > 0) {
              curCommandName = cmdString.substring(0, spaceIndex);
              curCommandArgument = cmdString.substring(spaceIndex + 1);
          } else {
              curCommandName = cmdString;
          }
          curCommandName = curCommandName.toUpperCase(Locale.US);

          if (helper.getLogger().isDebugEnabled()) {
              // Don't display password in logger
              if (!curCommandName.equals("PASS")) {
                  helper.getLogger().debug("Command received: " + cmdString);
              } else {
                  helper.getLogger().debug("Command received: PASS <password omitted>");
              }
          }

          //fetch the command handlers registered to the command
          List commandHandlers = handlerChain.getCommandHandlers(curCommandName);
          if(commandHandlers == null) {
              //end the session
              break;
          } else {
              int count = commandHandlers.size();
              for(int i = 0; i < count; i++) {
                  ((CommandHandler)commandHandlers.get(i)).onCommand(this);
                  helper.getWatchdog().reset();
                  //if the response is received, stop processing of command handlers
                  if(mode != COMMAND_MODE) {
                      break;
                  }
              }

          }
        }
        helper.getWatchdog().stop();
        if (helper.getLogger().isInfoEnabled()) {
            StringBuffer logBuffer =
                new StringBuffer(128)
                    .append("Connection for ")
                    .append(getUser())
                    .append(" from ")
                    .append(helper.getRemoteHost())
                    .append(" (")
                    .append(helper.getRemoteIP())
                    .append(") closed.");
            helper.getLogger().info(logBuffer.toString());
        }
    }
    
    /**
     * @see org.apache.james.socket.AbstractJamesHandler#errorHandler(java.lang.RuntimeException)
     */
    public void errorHandler(RuntimeException e) {
        helper.defaultErrorHandler(e);
        try {
            helper.getOutputWriter().println(ERR_RESPONSE + " Error closing connection.");
            helper.getOutputWriter().flush();
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
            Iterator i = userMailbox.iterator();
            while (i.hasNext()) {
                ContainerUtil.dispose(i.next());
            }
            userMailbox.clear();
            userMailbox = null;
        }

        if (backupUserMailbox != null) {
            Iterator i = backupUserMailbox.iterator();
            while (i.hasNext()) {
                ContainerUtil.dispose(i.next());
            }
            backupUserMailbox.clear();
            backupUserMailbox = null;
        }
    }

    /**
     * Reads a line of characters off the command line.
     *
     * @return the trimmed input line
     * @throws IOException if an exception is generated reading in the input characters
     */
    public final String readCommandLine() throws IOException {
        for (;;) try {
            String commandLine = helper.getInputReader().readLine();
            if (commandLine != null) {
                commandLine = commandLine.trim();
            }
            return commandLine;
        } catch (CRLFTerminatedReader.TerminationException te) {
            helper.writeLoggedFlushedResponse("-ERR Syntax error at character position " + te.position() + ". CR and LF must be CRLF paired.  See RFC 1939 #3.");
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
        return helper.getRemoteHost();
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return helper.getRemoteIP();
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
     * @see org.apache.james.pop3server.POP3Session#resetState()
     */
    public void resetState() {
        state.clear();
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getState()
     */
    public HashMap getState() {
        return state;
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
     * @see org.apache.james.pop3server.POP3Session#getResponseBuffer()
     */
    public StringBuffer getResponseBuffer() {
        return responseBuffer;
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
        return helper.getWatchdog();
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#writeResponse(java.lang.String)
     */
    public void writeResponse(String respString) {
        helper.writeLoggedFlushedResponse(respString);
        //TODO Explain this well
        if(mode == COMMAND_MODE) {
            mode = RESPONSE_MODE;
        }
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getCommandName()
     */
    public String getCommandName() {
        return curCommandName;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getCommandArgument()
     */
    public String getCommandArgument() {
        return curCommandArgument;
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
    public List getUserMailbox() {
        return userMailbox;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#setUserMailbox(java.util.List)
     */
    public void setUserMailbox(List userMailbox) {
        this.userMailbox = userMailbox;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getBackupUserMailbox()
     */
    public List getBackupUserMailbox() {
        return backupUserMailbox;
    }


    /**
     * @see org.apache.james.pop3server.POP3Session#setUserMailbox(List)
     */
    public void setBackupUserMailbox(List backupUserMailbox) {
        this.backupUserMailbox = backupUserMailbox;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getOutputStream()
     */
    public OutputStream getOutputStream() {
        return helper.getOutputStream();
    }

    /**
     * @see org.apache.james.socket.ProtocolHandler#setProtocolHandlerHelper(org.apache.james.socket.ProtocolHandlerHelper)
     */
    public void setProtocolHandlerHelper(ProtocolHandlerHelper phh) {
        this.helper = phh;
    }

}
