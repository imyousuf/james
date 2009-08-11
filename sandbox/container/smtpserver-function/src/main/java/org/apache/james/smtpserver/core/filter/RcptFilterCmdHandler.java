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



package org.apache.james.smtpserver.core.filter;

import java.util.ArrayList;
import java.util.Collection;

import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.mailet.MailAddress;

/**
  * Handles RCPT command
  */
public class RcptFilterCmdHandler extends AbstractLogEnabled implements
        CommandHandler {


    /**
     * handles RCPT command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
    **/
    public void onCommand(SMTPSession session) {
        doRCPT(session, session.getCommandArgument());
    }


    /**
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doRCPT(SMTPSession session, String argument) {
        String responseString = null;
        
        String recipient = null;
        if ((argument != null) && (argument.indexOf(":") > 0)) {
            int colonIndex = argument.indexOf(":");
            recipient = argument.substring(colonIndex + 1);
            argument = argument.substring(0, colonIndex);
        }
        if (!session.getState().containsKey(SMTPSession.SENDER)) {
            responseString = "503 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" Need MAIL before RCPT";
            session.writeResponse(responseString);
            
            // After this filter match we should not call any other handler!
            session.setStopHandlerProcessing(true);
            
        } else if (argument == null || !argument.toUpperCase(Locale.US).equals("TO")
                   || recipient == null) {
            responseString = "501 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_SYNTAX)+" Usage: RCPT TO:<recipient>";
            session.writeResponse(responseString);
            
            // After this filter match we should not call any other handler!
            session.setStopHandlerProcessing(true);
            
        } else {
            Collection rcptColl = (Collection) session.getState().get(SMTPSession.RCPT_LIST);
            if (rcptColl == null) {
                rcptColl = new ArrayList();
            }
            recipient = recipient.trim();
            int lastChar = recipient.lastIndexOf('>');
            // Check to see if any options are present and, if so, whether they are correctly formatted
            // (separated from the closing angle bracket by a ' ').
            String rcptOptionString = null;
            if ((lastChar > 0) && (recipient.length() > lastChar + 2) && (recipient.charAt(lastChar + 1) == ' ')) {
                rcptOptionString = recipient.substring(lastChar + 2);

                // Remove the options from the recipient
                recipient = recipient.substring(0, lastChar + 1);
            }
            if (session.getConfigurationData().useAddressBracketsEnforcement() && (!recipient.startsWith("<") || !recipient.endsWith(">"))) {
                responseString = "501 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_SYNTAX)+" Syntax error in parameters or arguments";
                session.writeResponse(responseString);
                if (getLogger().isErrorEnabled()) {
                    StringBuffer errorBuffer =
                        new StringBuffer(192)
                                .append("Error parsing recipient address: ")
                                .append("Address did not start and end with < >")
                                .append(getContext(session,null,recipient));
                    getLogger().error(errorBuffer.toString());
                }
                
                // After this filter match we should not call any other handler!
                session.setStopHandlerProcessing(true);
                
                return;
            }
            MailAddress recipientAddress = null;
            //Remove < and >
            if (session.getConfigurationData().useAddressBracketsEnforcement() || (recipient.startsWith("<") && recipient.endsWith(">"))) {
                recipient = recipient.substring(1, recipient.length() - 1);
            }
            
            if (recipient.indexOf("@") < 0) {
                // set the default domain
                recipient = recipient + "@" + session.getConfigurationData().getMailServer().getDefaultDomain();
            }
            
            try {
                recipientAddress = new MailAddress(recipient);
            } catch (Exception pe) {
                /*
                 * from RFC2822;
                 * 553 Requested action not taken: mailbox name not allowed
                 *     (e.g., mailbox syntax incorrect)
                 */
                responseString = "553 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.ADDRESS_SYNTAX)+" Syntax error in recipient address";
                session.writeResponse(responseString);

                if (getLogger().isErrorEnabled()) {
                    StringBuffer errorBuffer =
                        new StringBuffer(192)
                                .append("Error parsing recipient address: ")
                                .append(getContext(session,recipientAddress,recipient))
                                .append(pe.getMessage());
                    getLogger().error(errorBuffer.toString());
                }
                
                // After this filter match we should not call any other handler!
                session.setStopHandlerProcessing(true);
                
                return;
            }

            

            if (session.isAuthRequired() && !session.isRelayingAllowed()) {
                // Make sure the mail is being sent locally if not
                // authenticated else reject.
                if (session.getUser() == null) {
                    String toDomain = recipientAddress.getHost();
                    if (!session.getConfigurationData().getMailServer().isLocalServer(toDomain)) {
                        responseString = "530 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SECURITY_AUTH)+" Authentication Required";
                        session.writeResponse(responseString);
                        StringBuffer sb = new StringBuffer(128);
                        sb.append("Rejected message - authentication is required for mail request");
                        sb.append(getContext(session,recipientAddress,recipient));
                        getLogger().error(sb.toString());
                        
                        // After this filter match we should not call any other handler!
                        session.setStopHandlerProcessing(true);
                        
                        return;
                    }
                } else {
                    // Identity verification checking
                    if (session.getConfigurationData().isVerifyIdentity()) {
                        String authUser = (session.getUser()).toLowerCase(Locale.US);
                        MailAddress senderAddress = (MailAddress) session.getState().get(SMTPSession.SENDER);

                        if ((senderAddress == null) || (!authUser.equals(senderAddress.getUser())) ||
                            (!session.getConfigurationData().getMailServer().isLocalServer(senderAddress.getHost()))) {
                            responseString = "503 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SECURITY_AUTH)+" Incorrect Authentication for Specified Email Address";
                            session.writeResponse(responseString);
                            if (getLogger().isErrorEnabled()) {
                                StringBuffer errorBuffer =
                                    new StringBuffer(128)
                                        .append("User ")
                                        .append(authUser)
                                        .append(" authenticated, however tried sending email as ")
                                        .append(senderAddress)
                                        .append(getContext(session,recipientAddress,recipient));
                                getLogger().error(errorBuffer.toString());
                            }
                            
                            // After this filter match we should not call any other handler!
                            session.setStopHandlerProcessing(true);
                            
                            return;
                        }
                    }
                }
            } else if (!session.isRelayingAllowed()) {
                String toDomain = recipientAddress.getHost();
                if (!session.getConfigurationData().getMailServer().isLocalServer(toDomain)) {
                    responseString = "550 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SECURITY_AUTH)+" Requested action not taken: relaying denied";
                    session.writeResponse(responseString);
                    StringBuffer errorBuffer = new StringBuffer(128)
                        .append("Rejected message - ")
                        .append(session.getRemoteIPAddress())
                        .append(" not authorized to relay to ")
                        .append(toDomain)
                        .append(getContext(session,recipientAddress,recipient));
                    getLogger().error(errorBuffer.toString());
                    
                    // After this filter match we should not call any other handler!
                    session.setStopHandlerProcessing(true);
                    
                    return;
                }
            }
            if (rcptOptionString != null) {

              StringTokenizer optionTokenizer = new StringTokenizer(rcptOptionString, " ");
              while (optionTokenizer.hasMoreElements()) {
                  String rcptOption = optionTokenizer.nextToken();
                  int equalIndex = rcptOption.indexOf('=');
                  String rcptOptionName = rcptOption;
                  String rcptOptionValue = "";
                  if (equalIndex > 0) {
                      rcptOptionName = rcptOption.substring(0, equalIndex).toUpperCase(Locale.US);
                      rcptOptionValue = rcptOption.substring(equalIndex + 1);
                  }
                  // Unexpected option attached to the RCPT command
                  if (getLogger().isDebugEnabled()) {
                      StringBuffer debugBuffer =
                          new StringBuffer(128)
                              .append("RCPT command had unrecognized/unexpected option ")
                              .append(rcptOptionName)
                              .append(" with value ")
                              .append(rcptOptionValue)
                              .append(getContext(session,recipientAddress,recipient));
                      getLogger().debug(debugBuffer.toString());
                  }
                  
                  // After this filter match we should not call any other handler!
                  session.setStopHandlerProcessing(true);
                  
              }
              optionTokenizer = null;
            }
    
            session.getState().put(SMTPSession.CURRENT_RECIPIENT,recipientAddress);
        }
    }


    private String getContext(SMTPSession session, MailAddress recipientAddress, String recipient){
        StringBuffer sb = new StringBuffer(128);
        if(null!=recipientAddress) {
            sb.append(" [to:" + (recipientAddress).toInternetAddress().getAddress() + "]");
        } else if(null!=recipient) {
            sb.append(" [to:" + recipient + "]");
        }
        if (null!=session.getState().get(SMTPSession.SENDER)) {
            sb.append(" [from:" + ((MailAddress)session.getState().get(SMTPSession.SENDER)).toInternetAddress().getAddress() + "]");
        }
        return sb.toString();
    } 
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("RCPT");
        
        return implCommands;
    }
    
}
