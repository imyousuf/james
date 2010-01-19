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

package org.apache.james.smtpserver.protocol.core.esmtp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.james.api.protocol.CommandHandler;
import org.apache.james.api.protocol.Request;
import org.apache.james.api.protocol.Response;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;

/**
 * Handles STARTTLS command
 */
public class StartTlsCmdHandler implements CommandHandler<SMTPSession>, EhloExtension {
	/**
	 * The name of the command handled by the command handler
	 */
	private final static String COMMAND_NAME = "STARTTLS";

	/**
	 * @see org.apache.james.smtpserver.protocol.CommandHandler#getImplCommands()
	 */
	public Collection<String> getImplCommands() {
		Collection<String> commands = new ArrayList<String>();
		commands.add(COMMAND_NAME);
		return commands;
	}

	/**
	 * Handler method called upon receipt of a STARTTLS command. Resets
	 * message-specific, but not authenticated user, state.
	 * 
	 */
    public Response onCommand(SMTPSession session, Request request) {
		SMTPResponse response = null;
		String command = request.getCommand();
		String parameters = request.getArgument();
		if (session.isStartTLSSupported()) {
			if (session.isTLSStarted()) {
				response = new SMTPResponse("500", DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_CMD) + " TLS already active RFC2487 5.2");
			} else {
				if ((parameters == null) || (parameters.length() == 0)) {
					response = new SMTPResponse("220", DSNStatus.getStatus(DSNStatus.SUCCESS, DSNStatus.UNDEFINED_STATUS) + " Ready to start TLS");
				} else {
					response = new SMTPResponse("501 "+ DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_ARG) + " Syntax error (no parameters allowed) with STARTTLS command");
				}
				session.writeResponse(response);
				try {
					if (!session.isTLSStarted()) {
						session.startTLS();
						// force reset
						session.resetState();
					}
				} catch (IOException e) {
					return new SMTPResponse(SMTPRetCode.LOCAL_ERROR,"TLS not available due to temporary reason");
				}
			}
			
		} else {
	        StringBuilder result = new StringBuilder();
	        result.append(DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_CMD))
	                      .append(" Command ")
	                      .append(command)
	                      .append(" unrecognized.");
	        response =  new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_COMMAND_UNRECOGNIZED, result);
		}
		return null;
	}

	/**
	 * @see org.apache.james.smtpserver.protocol.core.esmtp.EhloExtension#getImplementedEsmtpFeatures(org.apache.james.smtpserver.protocol.SMTPSession)
	 */
	public List<String> getImplementedEsmtpFeatures(SMTPSession session) {
		List<String> esmtpextensions = new ArrayList<String>();
		// SMTP STARTTLS
		if (!session.isTLSStarted() && session.isStartTLSSupported()) {
			esmtpextensions.add("STARTTLS");
		}
		return esmtpextensions;

	}

}
