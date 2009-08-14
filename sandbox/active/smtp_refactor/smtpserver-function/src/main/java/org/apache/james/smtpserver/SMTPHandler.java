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



package org.apache.james.smtpserver;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.apache.james.Constants;
import org.apache.james.socket.AbstractJamesHandler;
import org.apache.james.socket.ProtocolHandler;
import org.apache.james.socket.ProtocolHandlerHelper;
import org.apache.james.util.CRLFDelimitedByteBuffer;
import org.apache.mailet.base.RFC822DateFormat;

/**
 * Provides SMTP functionality by carrying out the server side of the SMTP
 * interaction.
 *
 * @version CVS $Revision$ $Date$
 */
public class SMTPHandler implements ProtocolHandler, SMTPSession {

	    private ProtocolHandlerHelper helper;
	
	     /**
	     * The constants to indicate the current processing mode of the session
	     */
	    private final static byte COMMAND_MODE = 1;
	    private final static byte RESPONSE_MODE = 2;
	    private final static byte MESSAGE_RECEIVED_MODE = 3;
	    private final static byte MESSAGE_ABORT_MODE = 4;
	    private boolean sessionEnded = false;

	    /**
	    * SMTP Server identification string used in SMTP headers
	     */
	    private final static String SOFTWARE_TYPE = "JAMES SMTP Server " + Constants.SOFTWARE_VERSION;
	
	   /**
	      * Static Random instance used to generate SMTP ids
	      */
	     private final static Random random = new Random();
	 
	     /**
	     * Static RFC822DateFormat used to generate date headers
	     */
	     private final static RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();
	
	   /**
	    * The name of the currently parsed command
	    */
	    String curCommandName =  null;

	   /**
	    * The value of the currently parsed command
	    */
	   String curCommandArgument =  null;
	
    /**
     * The hash map holds states which should be used in the whole connection
     */
    private HashMap connectionState = new HashMap();
    
    /**
     * If not null every line is sent to this command handler instead
     * of the default "command parsing -> dipatching" procedure.
     */
    private LinkedList lineHandlers;

    /**
     * Connect Handlers
     */
    private LinkedList connectHandlers;

	private SMTPHandlerConfigurationData theConfigData;

	private boolean relayingAllowed;

	private boolean authSupported;

	private SMTPHandlerChain handlerChain;

	private String authenticatedUser;

	private String smtpID;

	

    /**
     * Set the configuration data for the handler
     *
     * @param theData the per-service configuration data for this handler
     */
    public void setConfigurationData(Object theData) {
        if (theData instanceof SMTPHandlerConfigurationData) {
            theConfigData = (SMTPHandlerConfigurationData) theData;
        } else {
            throw new IllegalArgumentException("Configuration object does not implement SMTPHandlerConfigurationData");
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.ProtocolHandler#handleProtocol()
     */
    public void handleProtocol() throws IOException {
        smtpID = random.nextInt(1024) + "";
        relayingAllowed = theConfigData.isRelayingAllowed(helper.getRemoteIP());
        authSupported = theConfigData.isAuthRequired(helper.getRemoteIP());

        // Both called in resetHandler, we don't need to call them again here.
        // sessionEnded = false;
        // resetState();
        // resetConnectionState();

        //the core in-protocol handling logic
        //run all the connection handlers, if it fast fails, end the session
        //parse the command command, look up for the list of command handlers
        //Execute each of the command handlers. If any command handlers writes
        //response then, End the subsequent command handler processing and
        //start parsing new command. Once the message is received, run all
        //the message handlers. The message handlers can either terminate
        //message or terminate session

        //At the beginning
        //mode = command_mode
        //once the commandHandler writes response, the mode is changed to RESPONSE_MODE.
        //This will cause to skip the subsequent command handlers configured for that command.
        //For instance:
        //There are 2 commandhandlers MailAddressChecker and MailCmdHandler for
        //MAIL command. If MailAddressChecker validation of the MAIL FROM
        //address is successful, the MailCmdHandlers will be executed.
        //Incase it fails, it has to write response. Once we write response
        //there is no need to execute the MailCmdHandler.
        //Next, Once MAIL message is received the DataCmdHandler and any other
        //equivalent commandHandler will call setMail method. this will change
        //he mode to MAIL_RECEIVED_MODE. This mode will trigger the message
        //handlers to be execute. Message handlers can abort message. In that case,
        //message will not spooled.

        //Session started - RUN all connect handlers
        if(connectHandlers != null) {
            int count = connectHandlers.size();
            for(int i = 0; i < count; i++) {
                ((ConnectHandler)connectHandlers.get(i)).onConnect(this);
                if(sessionEnded) {
                    break;
                }
            }
        }

        CRLFDelimitedByteBuffer bytebufferHandler = new CRLFDelimitedByteBuffer(helper.getInputStream());
        helper.getWatchdog().start();
        while(!sessionEnded) {
          //parse the command
          byte[] line =  null;
          try {
              line = bytebufferHandler.read();
          } catch (CRLFDelimitedByteBuffer.TerminationException e) {
              writeSMTPResponse(new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS, "Syntax error at character position " + e.position() + ". CR and LF must be CRLF paired.  See RFC 2821 #2.7.1."));
          } catch (CRLFDelimitedByteBuffer.LineLengthExceededException e) {
              writeSMTPResponse(new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_COMMAND_UNRECOGNIZED, "Line length exceeded. See RFC 2821 #4.5.3.1."));
          }
          if (line == null) {
              break;
          }

          if (lineHandlers.size() > 0) {
              ((LineHandler) lineHandlers.getLast()).onLine(this, line);
          } else {
              sessionEnded = true;
          }
          helper.getWatchdog().reset();
          
        }
        helper.getWatchdog().stop();
        helper.getAvalonLogger().debug("Closing socket.");
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#writeSMTPResponse(org.apache.james.smtpserver.SMTPResponse)
     */
    public void writeSMTPResponse(SMTPResponse response) {
        // Write a single-line or multiline response
        if (response != null) {
            if (response.getRawLine() != null) {
                helper.writeLoggedFlushedResponse(response.getRawLine());
            } else {
                // Iterator i = esmtpextensions.iterator();
                for (int k = 0; k < response.getLines().size(); k++) {
                    StringBuffer respBuff = new StringBuffer(256);
                    respBuff.append(response.getRetCode());
                    if (k == response.getLines().size() - 1) {
                        respBuff.append(" ");
                        respBuff.append(response.getLines().get(k));
                        helper.writeLoggedFlushedResponse(respBuff.toString());
                    } else {
                        respBuff.append("-");
                        respBuff.append(response.getLines().get(k));
                        helper.writeLoggedResponse(respBuff.toString());
                    }
                }
            }
            
            if (response.isEndSession()) {
                sessionEnded = true;
            }
        }
    }
    
    /**
     * Resets the handler data to a basic state.
     */
    public void resetHandler() {
        // not needed anymore because state is inside the connection state
        // resetState();
        resetConnectionState();

        // empty any previous line handler and add self (command dispatcher)
        // as the default.
        lineHandlers = handlerChain.getHandlers(LineHandler.class);

        authenticatedUser = null;
        smtpID = null;
        sessionEnded = false;
    }

   /**
     * Sets the SMTPHandlerChain
     *
     * @param handlerChain SMTPHandler object
     */
    public void setHandlerChain(SMTPHandlerChain handlerChain) {
        this.handlerChain = handlerChain;
        connectHandlers = handlerChain.getHandlers(ConnectHandler.class);
        lineHandlers = handlerChain.getHandlers(LineHandler.class);
    }


    /**
     * @see org.apache.james.smtpserver.SMTPSession#getRemoteHost()
     */
    public String getRemoteHost() {
        return helper.getRemoteHost();
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return helper.getRemoteIP();
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#resetState()
     */
    public void resetState() {
        // remember the ehlo mode between resets
        Object currentHeloMode = getState().get(CURRENT_HELO_MODE);

        getState().clear();

        // start again with the old helo mode
        if (currentHeloMode != null) {
            getState().put(CURRENT_HELO_MODE,currentHeloMode);
        }
    }

    /**
     * The hash map that holds variables for the SMTP message transfer in progress.
     *
     * This hash map should only be used to store variable set in a particular
     * set of sequential MAIL-RCPT-DATA commands, as described in RFC 2821.  Per
     * connection values should be stored as member variables in this class.
     * 
     * @see org.apache.james.smtpserver.SMTPSession#getState()
     */
    public Map getState() {
        Object res = getConnectionState().get(SMTPSession.SESSION_STATE_MAP);
        if (res == null || !(res instanceof Map)) {
            res = new HashMap();
            getConnectionState().put(SMTPSession.SESSION_STATE_MAP, res);
        }
        return (Map) res;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getConfigurationData()
     */
    public SMTPHandlerConfigurationData getConfigurationData() {
        return theConfigData;
    }
    /**
     * @see org.apache.james.smtpserver.SMTPSession#isRelayingAllowed()
     */
    public boolean isRelayingAllowed() {
        return relayingAllowed;
    }
    
    /**
     * @see org.apache.james.smtpserver.SMTPSession#setRelayingAllowed(boolean relayingAllowed)
     */
    public void setRelayingAllowed(boolean relayingAllowed) {
        this.relayingAllowed = relayingAllowed;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#isAuthSupported()
     */
    public boolean isAuthSupported() {
        return authSupported;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getUser()
     */
    public String getUser() {
        return authenticatedUser;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#setUser(String)
     */
    public void setUser(String userID) {
        authenticatedUser = userID;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getSessionID()
     */
    public String getSessionID() {
        return smtpID;
    }
    
    /**
     * @see org.apache.james.smtpserver.SMTPSession#getRcptCount()
     */
    public int getRcptCount() {
        int count = 0;

        // check if the key exists
        if (getState().get(SMTPSession.RCPT_LIST) != null) {
            count = ((Collection) getState().get(SMTPSession.RCPT_LIST)).size();
        }

        return count;
    }
    
    public void resetConnectionState() {
        connectionState.clear();
    }
    
    /**
     * @see org.apache.james.smtpserver.SMTPSession#getConnectionState()
     */
    public Map getConnectionState() {
        return connectionState;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#popLineHandler()
     */
    public void popLineHandler() {
        if (lineHandlers != null) {
            lineHandlers.removeLast();
        }
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#pushLineHandler(org.apache.james.smtpserver.LineHandler)
     */
    public void pushLineHandler(LineHandler lineHandler) {
        if (lineHandlers == null) {
            lineHandlers = new LinkedList();
        }
        lineHandlers.addLast(lineHandler);
    }
    
    /**
     * @see org.apache.james.smtpserver.SMTPSession#sleep(long)
     */
    public void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // ignore
        }
    }

	public void errorHandler(RuntimeException e) {
		helper.defaultErrorHandler(e);
	}

	public void setProtocolHandlerHelper(ProtocolHandlerHelper phh) {
		helper = phh;
	}

}
