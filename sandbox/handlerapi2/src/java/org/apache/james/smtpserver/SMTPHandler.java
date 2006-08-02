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

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.Constants;
import org.apache.james.core.AbstractJamesHandler;
import org.apache.james.util.CRLFTerminatedReader;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.mailet.Mail;
import org.apache.mailet.dates.RFC822DateFormat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Provides SMTP functionality by carrying out the server side of the SMTP
 * interaction.
 *
 * @version CVS $Revision$ $Date$
 */
public class SMTPHandler
    extends AbstractJamesHandler
    implements SMTPSession {

    /**
     * The constants to indicate the current processing mode of the session
     */
    private final static byte COMMAND_MODE = 1;
    private final static byte RESPONSE_MODE = 2;
    private final static byte MESSAGE_RECEIVED_MODE = 3;
    private final static byte MESSAGE_ABORT_MODE = 4;

    /**
     * SMTP Server identification string used in SMTP headers
     */
    private final static String SOFTWARE_TYPE = "JAMES SMTP Server "
                                                 + Constants.SOFTWARE_VERSION;

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
     * The SMTPHandlerChain object set by SMTPServer
     */
    SMTPHandlerChain handlerChain = null;


    /**
     * The mode of the current session
     */
    private byte mode;

    /**
     * The MailImpl object set by the DATA command
     */
    private Mail mail = null;

    /**
     * The session termination status
     */
    private boolean sessionEnded = false;

    /**
     * The user name of the authenticated user associated with this SMTP transaction.
     */
    private String authenticatedUser;

    /**
     * whether or not authorization is required for this connection
     */
    private boolean authRequired;

    /**
     * whether or not this connection can relay without authentication
     */
    private boolean relayingAllowed;

    /**
     * Whether the remote Server must send HELO/EHLO 
     */
    private boolean heloEhloEnforcement;
    

    /**
     * The SMTPGreeting
     */
    private String smtpGreeting = null;
    
    /**
     * The id associated with this particular SMTP interaction.
     */
    private String smtpID;

    /**
     * The per-service configuration data that applies to all handlers
     */
    private SMTPHandlerConfigurationData theConfigData;

    /**
     * The hash map that holds variables for the SMTP message transfer in progress.
     *
     * This hash map should only be used to store variable set in a particular
     * set of sequential MAIL-RCPT-DATA commands, as described in RFC 2821.  Per
     * connection values should be stored as member variables in this class.
     */
    private HashMap state = new HashMap();

    /**
     * The hash map holds states which should be used in the whole connection
     */
    private HashMap connectionState = new HashMap();
    
    /**
     * The per-handler response buffer used to marshal responses.
     */
    private StringBuffer responseBuffer = new StringBuffer(256);
    
    private final static int DEFAULT_SMTP_CODE = 500;
    private final static String DEFAULT_SMTP_RESPONSE = "Unexpected Error";
    
    private SMTPResponse response = new SMTPResponse(DEFAULT_SMTP_CODE,DEFAULT_SMTP_RESPONSE);

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
    
    /**
     * @see org.apache.james.core.AbstractJamesHandler#handleProtocol()
     */
    protected void handleProtocol() throws IOException {
        smtpID = random.nextInt(1024) + "";
        relayingAllowed = theConfigData.isRelayingAllowed(remoteIP);
        authRequired = theConfigData.isAuthRequired(remoteIP);
        heloEhloEnforcement = theConfigData.useHeloEhloEnforcement();
        sessionEnded = false;
        smtpGreeting = theConfigData.getSMTPGreeting();
        resetState();
        resetConnectionState();

        // if no greeting was configured use a default
        if (smtpGreeting == null) {
            // Initially greet the connector
            // Format is:  Sat, 24 Jan 1998 13:16:09 -0500

            responseBuffer.append("220 ")
                          .append(theConfigData.getHelloName())
                          .append(" SMTP Server (")
                          .append(SOFTWARE_TYPE)
                          .append(") ready ")
                          .append(rfc822DateFormat.format(new Date()));
        } else {
            responseBuffer.append("220 ")
                          .append(smtpGreeting);
        }
        String responseString = clearResponseBuffer();
        writeLoggedFlushedResponse(responseString);

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
        List connectHandlers = handlerChain.getConnectHandlers();
        if(connectHandlers != null) {
            new Chain(connectHandlers.iterator()).doChain(this);
        }

        theWatchdog.start();
        while (!sessionEnded) {
	    // Reset the current command values
	    curCommandName = null;
	    curCommandArgument = null;
	    mode = COMMAND_MODE;

	    // parse the command
	    String cmdString = readCommandLine();
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

	    // fetch the command handlers registered to the command
	    List commandHandlers = handlerChain
		    .getCommandHandlers(curCommandName);
	    if (commandHandlers == null) {
		// end the session
		break;
	    } else {
		new Chain(commandHandlers.iterator()).doChain(this);

		writeCompleteResponse(getSMTPResponse());
	    }

	    // handle messages
	    if (mode == MESSAGE_RECEIVED_MODE) {
		getLogger().debug("executing message handlers");
		List messageHandlers = handlerChain.getMessageHandlers();

		if (messageHandlers != null) {
		    new Chain(messageHandlers.iterator()).doChain(this);

		    writeCompleteResponse(getSMTPResponse());
		}
	    }

	    // do the clean up
	    if (mail != null) {
		ContainerUtil.dispose(mail);

		// remember the ehlo mode
		Object currentHeloMode = state.get(CURRENT_HELO_MODE);

		mail = null;
		resetState();

		// start again with the old helo mode
		if (currentHeloMode != null) {
		    state.put(CURRENT_HELO_MODE, currentHeloMode);
		}
	    }

	}
	theWatchdog.stop();
	getLogger().debug("Closing socket.");
    }

    /**
     * Write a Collection of responseString to the client
     * 
     * @param resp
     *                The Collection of responseStrings
     */
    private void writeCompleteResponse(SMTPResponse responses) {
	if (responses == null) return;
	
	Collection resp = response.getSMTPResponse();
	
	if (resp.size() > 0) {
	    Iterator response = resp.iterator();

	    while (response.hasNext()) {
		String responseString = response.next().toString();
		String finalResponse = null;
		
		if (response.hasNext()) {
		finalResponse = responses.getSMTPCode() + "-" + responseString;
		} else {
		    finalResponse = responses.getSMTPCode() + " " + responseString;
		}
		writeResponse(finalResponse);
	    }
	}
	resetSMTPResponse();
    }
    
    /**
     * Resets the handler data to a basic state.
     */
    protected void resetHandler() {
	this.response = null;
	resetState();
	resetConnectionState();

	clearResponseBuffer();

	remoteHost = null;
	remoteIP = null;
	authenticatedUser = null;
	smtpID = null;
	resetSMTPResponse();
    }

   /**
    * Sets the SMTPHandlerChain
    * 
    * @param handlerChain
    *                SMTPHandler object
    */
    public void setHandlerChain(SMTPHandlerChain handlerChain) {
        this.handlerChain = handlerChain;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#writeResponse(String)
     */
    public void writeResponse(String respString) {
        writeLoggedFlushedResponse(respString);
      
        //TODO Explain this well
        if(mode == COMMAND_MODE) {
            mode = RESPONSE_MODE;
        }
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getCommandName()
     */
    public String getCommandName() {
        return curCommandName;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getCommandArgument()
     */
    public String getCommandArgument() {
        return curCommandArgument;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getMail()
     */
    public Mail getMail() {
        return mail;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#setMail(Mail)
     */
    public void setMail(Mail mail) {
        this.mail = mail;
        this.mode = MESSAGE_RECEIVED_MODE;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getRemoteHost()
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return remoteIP;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#endSession()
     */
    public void endSession() {
        sessionEnded = true;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#isSessionEnded()
     */
    public boolean isSessionEnded() {
        return sessionEnded;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#resetState()
     */
    public void resetState() {
        ArrayList recipients = (ArrayList)state.get(RCPT_LIST);
        if (recipients != null) {
            recipients.clear();
        }
        state.clear();
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getState()
     */
    public Map getState() {
        return state;
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
     * @see org.apache.james.smtpserver.SMTPSession#isAuthRequired()
     */
    public boolean isAuthRequired() {
        return authRequired;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#useHeloEhloEnforcement()
     */
    public boolean useHeloEhloEnforcement() {
        return heloEhloEnforcement;
    }
    /**
     * @see org.apache.james.smtpserver.SMTPSession#getUser()
     */
    public String getUser() {
        return authenticatedUser;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#setUser()
     */
    public void setUser(String userID) {
        authenticatedUser = userID;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getResponseBuffer()
     */
    public StringBuffer getResponseBuffer() {
        return responseBuffer;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#clearResponseBuffer()
     */
    public String clearResponseBuffer() {
        String responseString = responseBuffer.toString();
        responseBuffer.delete(0,responseBuffer.length());
        return responseString;
    }


    /**
     * @see org.apache.james.smtpserver.SMTPSession#readCommandLine()
     */
    public final String readCommandLine() throws IOException {
        for (;;) try {
            String commandLine = inReader.readLine();
            if (commandLine != null) {
                commandLine = commandLine.trim();
            }
            return commandLine;
        } catch (CRLFTerminatedReader.TerminationException te) {
            writeLoggedFlushedResponse("501 Syntax error at character position " + te.position() + ". CR and LF must be CRLF paired.  See RFC 2821 #2.7.1.");
        } catch (CRLFTerminatedReader.LineLengthExceededException llee) {
            writeLoggedFlushedResponse("500 Line length exceeded. See RFC 2821 #4.5.3.1.");
        }
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getWatchdog()
     */
    public Watchdog getWatchdog() {
        return theWatchdog;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getInputStream()
     */
    public InputStream getInputStream() {
        return in;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#getSessionID()
     */
    public String getSessionID() {
        return smtpID;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#abortMessage()
     */
    public void abortMessage() {
        mode = MESSAGE_ABORT_MODE;
    }
    
    /**
     * @see org.apache.james.smtpserver.SMTPSession#getRcptCount()
     */
    public int getRcptCount() {
        int count = 0;

        // check if the key exists
        if (state.get(SMTPSession.RCPT_LIST) != null) {
            count = ((Collection) state.get(SMTPSession.RCPT_LIST)).size();
        }

        return count;
    }
    
    /**
     * @see org.apache.james.smtpserver.SMTPSession#resetConnectionState()
     */
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
     * @see org.apache.james.smtpserver.SMTPSession#getSMTPResponse()
     */
    public SMTPResponse getSMTPResponse() {
	return response;
    }
    
    /**
     * @see org.apache.james.smtpserver.SMTPSession#setSMTPResponse(SMTPResponse)
     */
    public void setSMTPResponse(SMTPResponse response){
	this.response = response;
    }
    
    /**
     * Reset the SMTPResponse to the default state
     */
    private void resetSMTPResponse() {
	getSMTPResponse().setSMTPCode(DEFAULT_SMTP_CODE);
	getSMTPResponse().setSMTPResponse(DEFAULT_SMTP_RESPONSE);
    }

}
