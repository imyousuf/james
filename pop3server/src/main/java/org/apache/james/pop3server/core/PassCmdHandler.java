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



package org.apache.james.pop3server.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.apache.james.api.user.UsersRepository;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.james.util.POP3BeforeSMTPHelper;

/**
  * Handles PASS command
  */
public class PassCmdHandler extends RsetCmdHandler {

	private final static String COMMAND_NAME ="PASS";
	private UsersRepository users;
	private MailServer mailServer;


    /**
     * Sets the users repository.
     * @param users the users to set
     */
    @Resource(name="localusersrepository")
    public final void setUsers(UsersRepository users) {
        this.users = users;
    }
	
    /**
     * Sets the mail server.
     * @param mailServer the mailServer to set
     */
    @Resource(name="James")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    
    
	/**
     * Handler method called upon receipt of a PASS command.
     * Reads in and validates the password.
     *
	 */
    public Response onCommand(POP3Session session, Request request) {
    	String parameters = request.getArgument();
        POP3Response response = null;
        if (session.getHandlerState() == POP3Session.AUTHENTICATION_USERSET && parameters != null) {
            String passArg = parameters;
            if (users.test(session.getUser(), passArg)) {
                try {
                    MailRepository inbox = mailServer.getUserInbox(session.getUser());
                    if (inbox == null) {
                        throw new IllegalStateException("MailServer returned a null inbox for "+session.getUser());
                    }
                    session.setUserInbox(inbox);
                    stat(session);
                    
                    // Store the ipAddress to use it later for pop before smtp 
                    POP3BeforeSMTPHelper.addIPAddress(session.getRemoteIPAddress());
                    
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append("Welcome ")
                                .append(session.getUser());
                    response = new POP3Response(POP3Response.OK_RESPONSE,responseBuffer.toString());
                    session.setHandlerState(POP3Session.TRANSACTION);
                } catch (RuntimeException e) {
                    session.getLogger().error("Unexpected error accessing mailbox for "+session.getUser(),e);
                    response = new POP3Response(POP3Response.ERR_RESPONSE,"Unexpected error accessing mailbox");
                    session.setHandlerState(POP3Session.AUTHENTICATION_READY);
                }
            } else {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Authentication failed.");

                session.setHandlerState(POP3Session.AUTHENTICATION_READY);
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
        }
        return response;    
    }

  

    /**
     * @see org.apache.james.api.protocol.CommonCommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }


}
