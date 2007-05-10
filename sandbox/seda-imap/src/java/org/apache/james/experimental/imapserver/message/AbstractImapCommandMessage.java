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
package org.apache.james.experimental.imapserver.message;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.store.MailboxException;

abstract class AbstractImapCommandMessage extends AbstractLogEnabled implements ImapCommandMessage {
    private final String tag;
    private final ImapCommand command;
    
    public AbstractImapCommandMessage(final String tag, final ImapCommand command) {
        this.tag = tag;
        this.command = command;
    }
    public ImapResponseMessage process(ImapSession session) {
        ImapResponseMessage result;
        final Logger logger = getLogger();
        try {
            result = doProcess(session);
        }
        catch ( MailboxException e ) {
            if (logger != null) {
                logger.debug("error processing command ", e);
            }
            result = new CommandFailedResponseMessage( command, e.getResponseCode(), 
                    e.getMessage(), tag );
        }
        catch ( AuthorizationException e ) {
            if (logger != null) {
                logger.debug("error processing command ", e);
            }
            String msg = "Authorization error: Lacking permissions to perform requested operation.";
            result = new CommandFailedResponseMessage( command, null, 
                    msg, tag );
        }
        catch ( ProtocolException e ) {
            if (logger != null) {
                logger.debug("error processing command ", e);
            }
            String msg = e.getMessage() + " Command should be '" +
                    command.getExpectedMessage() + "'";
            result = new ErrorResponseMessage( msg, tag );
        }
        return result;
    }
    
    final ImapResponseMessage doProcess(ImapSession session) throws MailboxException, AuthorizationException, ProtocolException {
        ImapResponseMessage result;
        if ( !command.validForState( session.getState() ) ) {
            result = 
                new CommandFailedResponseMessage(command, 
                        "Command not valid in this state", tag );
        } else {
            result = doProcess( session, tag, command );
        }
        return result;
    }
    
    protected abstract ImapResponseMessage doProcess(ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException;
}
