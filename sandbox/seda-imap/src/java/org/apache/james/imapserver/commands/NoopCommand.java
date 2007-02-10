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

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ProtocolException;

/**
 * Handles processeing for the NOOP imap command.
 *
 * @version $Revision: 109034 $
 */
class NoopCommand extends CommandTemplate
{
    public static final String NAME = "NOOP";
    public static final String ARGS = null;
    
    private final NoopCommandParser parser = new NoopCommandParser(this);
    
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

    protected AbstractImapCommandMessage decode(ImapRequestLineReader request, String tag) throws ProtocolException {
        final AbstractImapCommandMessage result = parser.decode(request, tag);
        return result;
    }
    
    private static class NoopCommandParser extends CommandParser {

        public NoopCommandParser(ImapCommand command) {
            super(command);
        }

        protected AbstractImapCommandMessage decode(ImapCommand command, ImapRequestLineReader request, String tag) throws ProtocolException {
            endLine( request );
            final CompleteCommandMessage result = new CompleteCommandMessage(command, false, tag);
            return result;
        }
        
    }
}

/*
6.1.2.  NOOP Command

   Arguments:  none

   Responses:  no specific responses for this command (but see below)

   Result:     OK - noop completed
               BAD - command unknown or arguments invalid

      The NOOP command always succeeds.  It does nothing.

      Since any command can return a status update as untagged data, the
      NOOP command can be used as a periodic poll for new messages or
      message status updates during a period of inactivity.  The NOOP
      command can also be used to reset any inactivity autologout timer
      on the server.

   Example:    C: a002 NOOP
               S: a002 OK NOOP completed
                  . . .
               C: a047 NOOP
               S: * 22 EXPUNGE
               S: * 23 EXISTS
               S: * 3 RECENT
               S: * 14 FETCH (FLAGS (\Seen \Deleted))
               S: a047 OK NOOP completed
*/
