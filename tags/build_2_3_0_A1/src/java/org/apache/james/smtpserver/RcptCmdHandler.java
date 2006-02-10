/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.smtpserver;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.MailAddress;
import java.util.Collection;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Locale;

/**
  * Handles RCPT command
  */
public class RcptCmdHandler
    extends AbstractLogEnabled
    implements CommandHandler {

    /**
     * The keys used to store sender and recepients in the SMTPSession state
     */
    private final static String SENDER = "SENDER_ADDRESS";     // Sender's email address
    private final static String RCPT_LIST = "RCPT_LIST";   // The message recipients

    /*
     * handles RCPT command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
    **/
    public void onCommand(SMTPSession session) {
        doRCPT(session, session.getCommandArgument());
    }


    /**
     * Handler method called upon receipt of a RCPT command.
     * Reads recipient.  Does some connection validation.
     *
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doRCPT(SMTPSession session, String argument) {
        String responseString = null;
        StringBuffer responseBuffer = session.getResponseBuffer();

        String recipient = null;
        if ((argument != null) && (argument.indexOf(":") > 0)) {
            int colonIndex = argument.indexOf(":");
            recipient = argument.substring(colonIndex + 1);
            argument = argument.substring(0, colonIndex);
        }
        if (!session.getState().containsKey(SENDER)) {
            responseString = "503 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" Need MAIL before RCPT";
            session.writeResponse(responseString);
        } else if (argument == null || !argument.toUpperCase(Locale.US).equals("TO")
                   || recipient == null) {
            responseString = "501 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_SYNTAX)+" Usage: RCPT TO:<recipient>";
            session.writeResponse(responseString);
        } else {
            Collection rcptColl = (Collection) session.getState().get(RCPT_LIST);
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
            if (!recipient.startsWith("<") || !recipient.endsWith(">")) {
                responseString = "501 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_SYNTAX)+" Syntax error in parameters or arguments";
                session.writeResponse(responseString);
                if (getLogger().isErrorEnabled()) {
                    StringBuffer errorBuffer =
                        new StringBuffer(192)
                                .append("Error parsing recipient address: ")
                                .append(recipient)
                                .append(": did not start and end with < >");
                    getLogger().error(errorBuffer.toString());
                }
                return;
            }
            MailAddress recipientAddress = null;
            //Remove < and >
            recipient = recipient.substring(1, recipient.length() - 1);
            if (recipient.indexOf("@") < 0) {
                recipient = recipient + "@localhost";
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
                                .append(recipient)
                                .append(": ")
                                .append(pe.getMessage());
                    getLogger().error(errorBuffer.toString());
                }
                return;
            }


            if (session.isBlockListed() &&                                                // was found in the RBL
                (!session.isRelayingAllowed() || (session.isAuthRequired() && session.getUser() == null)) &&  // Not an authorized IP or SMTP AUTH is enabled and not authenticated
                !(recipientAddress.getUser().equalsIgnoreCase("postmaster") || recipientAddress.getUser().equalsIgnoreCase("abuse"))) {
                // trying to send e-mail to other than postmaster or abuse
                responseString = "530 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SECURITY_AUTH)+" Rejected: unauthenticated e-mail from " + session.getRemoteIPAddress() + " is restricted.  Contact the postmaster for details.";
                session.writeResponse(responseString);
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
                        getLogger().error("Rejected message - authentication is required for mail request");
                        return;
                    }
                } else {
                    // Identity verification checking
                    if (session.getConfigurationData().isVerifyIdentity()) {
                        String authUser = (session.getUser()).toLowerCase(Locale.US);
                        MailAddress senderAddress = (MailAddress) session.getState().get(SENDER);

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
                                        .append(senderAddress);
                                getLogger().error(errorBuffer.toString());
                            }
                            return;
                        }
                    }
                }
            } else if (!session.isRelayingAllowed()) {
                String toDomain = recipientAddress.getHost();
                if (!session.getConfigurationData().getMailServer().isLocalServer(toDomain)) {
                    responseString = "550 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SECURITY_AUTH)+" Requested action not taken: relaying denied";
                    session.writeResponse(responseString);
                    getLogger().error("Rejected message - " + session.getRemoteIPAddress() + " not authorized to relay to " + toDomain);
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
                              .append(rcptOptionValue);
                      getLogger().debug(debugBuffer.toString());
                  }
              }
              optionTokenizer = null;
            }
            rcptColl.add(recipientAddress);
            session.getState().put(RCPT_LIST, rcptColl);
            responseBuffer.append("250 "+DSNStatus.getStatus(DSNStatus.SUCCESS,DSNStatus.ADDRESS_VALID)+" Recipient <")
                          .append(recipient)
                          .append("> OK");
            responseString = session.clearResponseBuffer();
            session.writeResponse(responseString);
        }
    }


}
