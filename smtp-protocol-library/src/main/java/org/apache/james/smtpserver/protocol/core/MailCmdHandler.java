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

package org.apache.james.smtpserver.protocol.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.james.api.protocol.Request;
import org.apache.james.api.protocol.Response;
import org.apache.james.api.protocol.RetCodeResponse;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.MailHook;
import org.apache.james.smtpserver.protocol.hook.MailParametersHook;
import org.apache.mailet.MailAddress;

/**
 * Handles MAIL command
 */
public class MailCmdHandler extends AbstractHookableCmdHandler<MailHook> {

    /**
     * A map of parameterHooks
     */
    private Map<String, MailParametersHook> paramHooks;


    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#onCommand(org.apache.james.smtpserver.protocol.SMTPSession, org.apache.james.api.protocol.Request)
     */
	public Response onCommand(SMTPSession session, Request request) {
	    Response response =  super.onCommand(session, request);
		// Check if the response was not ok 
		if (((RetCodeResponse)response).getRetCode().equals(SMTPRetCode.MAIL_OK) == false) {
			// cleanup the session
			session.getState().remove(SMTPSession.SENDER);
		}
		
		return response;
	}

	/**
     * Handler method called upon receipt of a MAIL command. Sets up handler to
     * deliver mail as the stated sender.
     * 
     * @param session
     *            SMTP session object
     * @param argument
     *            the argument passed in with the command by the SMTP client
     */
    private SMTPResponse doMAIL(SMTPSession session, String argument) {
        StringBuilder responseBuffer = new StringBuilder();
        MailAddress sender = (MailAddress) session.getState().get(
                SMTPSession.SENDER);
        responseBuffer.append(
                DSNStatus.getStatus(DSNStatus.SUCCESS, DSNStatus.ADDRESS_OTHER))
                .append(" Sender <");
        if (sender != null) {
            responseBuffer.append(sender);
        }
        responseBuffer.append("> OK");
        return new SMTPResponse(SMTPRetCode.MAIL_OK, responseBuffer);
    }

    /**
     * @see org.apache.james.smtpserver.protocol.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        Collection<String> implCommands = new ArrayList<String>();
        implCommands.add("MAIL");

        return implCommands;
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#doCoreCmd(org.apache.james.smtpserver.protocol.SMTPSession,
     *      java.lang.String, java.lang.String)
     */
    protected SMTPResponse doCoreCmd(SMTPSession session, String command,
            String parameters) {
        return doMAIL(session, parameters);
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#doFilterChecks(org.apache.james.smtpserver.protocol.SMTPSession,
     *      java.lang.String, java.lang.String)
     */
    protected SMTPResponse doFilterChecks(SMTPSession session, String command,
            String parameters) {
        return doMAILFilter(session, parameters);
    }

    /**
     * @param session
     *            SMTP session object
     * @param argument
     *            the argument passed in with the command by the SMTP client
     */
    private SMTPResponse doMAILFilter(SMTPSession session, String argument) {
        String sender = null;

        if ((argument != null) && (argument.indexOf(":") > 0)) {
            int colonIndex = argument.indexOf(":");
            sender = argument.substring(colonIndex + 1);
            argument = argument.substring(0, colonIndex);
        }
        if (session.getState().containsKey(SMTPSession.SENDER)) {
            return new SMTPResponse(SMTPRetCode.BAD_SEQUENCE, DSNStatus
                    .getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_OTHER)
                    + " Sender already specified");
        } else if (!session.getConnectionState().containsKey(
                SMTPSession.CURRENT_HELO_MODE)
                && session.useHeloEhloEnforcement()) {
            return new SMTPResponse(SMTPRetCode.BAD_SEQUENCE, DSNStatus
                    .getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_OTHER)
                    + " Need HELO or EHLO before MAIL");
        } else if (argument == null
                || !argument.toUpperCase(Locale.US).equals("FROM")
                || sender == null) {
            return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS,
                    DSNStatus.getStatus(DSNStatus.PERMANENT,
                            DSNStatus.DELIVERY_INVALID_ARG)
                            + " Usage: MAIL FROM:<sender>");
        } else {
            sender = sender.trim();
            // the next gt after the first lt ... AUTH may add more <>
            int lastChar = sender.indexOf('>', sender.indexOf('<'));
            // Check to see if any options are present and, if so, whether they
            // are correctly formatted
            // (separated from the closing angle bracket by a ' ').
            if ((lastChar > 0) && (sender.length() > lastChar + 2)
                    && (sender.charAt(lastChar + 1) == ' ')) {
                String mailOptionString = sender.substring(lastChar + 2);

                // Remove the options from the sender
                sender = sender.substring(0, lastChar + 1);

                StringTokenizer optionTokenizer = new StringTokenizer(
                        mailOptionString, " ");
                while (optionTokenizer.hasMoreElements()) {
                    String mailOption = optionTokenizer.nextToken();
                    int equalIndex = mailOption.indexOf('=');
                    String mailOptionName = mailOption;
                    String mailOptionValue = "";
                    if (equalIndex > 0) {
                        mailOptionName = mailOption.substring(0, equalIndex)
                                .toUpperCase(Locale.US);
                        mailOptionValue = mailOption.substring(equalIndex + 1);
                    }

                    // Handle the SIZE extension keyword

                    if (paramHooks.containsKey(mailOptionName)) {
                        MailParametersHook hook = paramHooks.get(mailOptionName);
                        SMTPResponse res = calcDefaultSMTPResponse(hook.doMailParameter(session, mailOptionName, mailOptionValue));
                        if (res != null) {
                            return res;
                        }
                    } else {
                        // Unexpected option attached to the Mail command
                        if (session.getLogger().isDebugEnabled()) {
                            StringBuilder debugBuffer = new StringBuilder(128)
                                    .append(
                                            "MAIL command had unrecognized/unexpected option ")
                                    .append(mailOptionName).append(
                                            " with value ").append(
                                            mailOptionValue);
                            session.getLogger().debug(debugBuffer.toString());
                        }
                    }
                }
            }
            if (session.useAddressBracketsEnforcement()
                    && (!sender.startsWith("<") || !sender.endsWith(">"))) {
                if (session.getLogger().isErrorEnabled()) {
                    StringBuilder errorBuffer = new StringBuilder(128).append(
                            "Error parsing sender address: ").append(sender)
                            .append(": did not start and end with < >");
                    session.getLogger().error(errorBuffer.toString());
                }
                return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS,
                        DSNStatus.getStatus(DSNStatus.PERMANENT,
                                DSNStatus.ADDRESS_SYNTAX_SENDER)
                                + " Syntax error in MAIL command");
            }
            MailAddress senderAddress = null;

            if (session.useAddressBracketsEnforcement()
                    || (sender.startsWith("<") && sender.endsWith(">"))) {
                // Remove < and >
                sender = sender.substring(1, sender.length() - 1);
            }

            if (sender.length() == 0) {
                // This is the <> case. Let senderAddress == null
            } else {

                if (sender.indexOf("@") < 0) {
                    sender = sender
                            + "@"
                            + getDefaultDomain();
                }

                try {
                    senderAddress = new MailAddress(sender);
                } catch (Exception pe) {
                    if (session.getLogger().isErrorEnabled()) {
                        StringBuilder errorBuffer = new StringBuilder(256)
                                .append("Error parsing sender address: ")
                                .append(sender).append(": ").append(
                                        pe.getMessage());
                        session.getLogger().error(errorBuffer.toString());
                    }
                    return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS,
                            DSNStatus.getStatus(DSNStatus.PERMANENT,
                                    DSNStatus.ADDRESS_SYNTAX_SENDER)
                                    + " Syntax error in sender address");
                }
            }

            // Store the senderAddress in session map
            session.getState().put(SMTPSession.SENDER, senderAddress);
        }
        return null;
    }
    /**
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#getHookInterface()
     */
    protected Class<MailHook> getHookInterface() {
        return MailHook.class;
    }


    /**
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#callHook(java.lang.Object, org.apache.james.smtpserver.protocol.SMTPSession, java.lang.String)
     */
    protected HookResult callHook(MailHook rawHook, SMTPSession session, String parameters) {
        return rawHook.doMail(session,(MailAddress) session.getState().get(SMTPSession.SENDER));
    }

    
    /**
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#getMarkerInterfaces()
     */
    public List<Class<?>> getMarkerInterfaces() {
        List<Class<?>> l = super.getMarkerInterfaces();
        l.add(MailParametersHook.class);
        return l;
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    @SuppressWarnings("unchecked")
    public void wireExtensions(Class interfaceName, List extension) {
        if (MailParametersHook.class.equals(interfaceName)) {
            this.paramHooks = new HashMap<String, MailParametersHook>();
            for (Iterator<MailParametersHook> i = extension.iterator(); i.hasNext(); ) {
                MailParametersHook hook =  i.next();
                String[] params = hook.getMailParamNames();
                for (int k = 0; k < params.length; k++) {
                    paramHooks.put(params[k], hook);
                }
            }
        } else {
            super.wireExtensions(interfaceName, extension);
        }
    }

    /**
     * Return the default domain to append if the sender contains none
     * 
     * @return defaultDomain
     */
    protected String getDefaultDomain() {
        return "localhost";
    }

}
