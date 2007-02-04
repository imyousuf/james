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

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.store.MailboxException;

/**
 * Handles processeing for the LOGOUT imap command.
 *
 * @version $Revision: 109034 $
 */
class LogoutCommand extends CommandTemplate
{
    public static final String NAME = "LOGOUT";
    public static final String ARGS = null;
    public static final String BYE_MESSAGE = VERSION + SP + "Server logging out";

    private final LogoutCommandMessage message = new LogoutCommandMessage();
    private final LogoutResponseMessage response = new LogoutResponseMessage(this);
    
    /** @see ImapCommand#getName */
    public String getName()
    {
        return NAME;
    }

    /** @see CommandTemplate#getArgSyntax */
    public String getArgSyntax()
    {
        return ARGS;
    }

    protected AbstractImapCommandMessage decode(ImapRequestLineReader request) throws ProtocolException {
        parser.endLine( request );
        return message;
    }
    
    private class LogoutCommandMessage extends AbstractImapCommandMessage {

        protected ImapResponseMessage doProcess(ImapSession session) throws MailboxException, AuthorizationException, ProtocolException {
            return response;
        }
        
    }
    
    private static class LogoutResponseMessage extends AbstractCommandResponseMessage implements ImapCommandMessage {

        public LogoutResponseMessage(ImapCommand command) {
            super(command);
        }

        void doEncode(ImapResponse response, ImapSession session, ImapCommand command) throws MailboxException {
            response.byeResponse( BYE_MESSAGE );
            response.commandComplete( command );
            // TODO: think about how this will work with SEDA
            session.closeConnection();            
        }

        public ImapResponseMessage process(ImapSession session) {
            return this;
        }
    }
}

/*
6.1.3.  LOGOUT Command

   Arguments:  none

   Responses:  REQUIRED untagged response: BYE

   Result:     OK - logout completed
               BAD - command unknown or arguments invalid

      The LOGOUT command informs the server that the client is done with
      the connection.  The server MUST send a BYE untagged response
      before the (tagged) OK response, and then close the network
      connection.

   Example:    C: A023 LOGOUT
               S: * BYE IMAP4rev1 Server logging out
               S: A023 OK LOGOUT completed
               (Server and client then close the connection)
*/
