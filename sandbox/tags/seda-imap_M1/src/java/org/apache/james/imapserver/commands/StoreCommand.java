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

import javax.mail.Flags;

import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;

/**
 * Handles processeing for the STORE imap command.
 *
 * @version $Revision: 109034 $
 */
class StoreCommand extends SelectedStateCommand implements UidEnabledCommand
{
    public static final String NAME = "STORE";
    public static final String ARGS = "<Message-set> ['+'|'-']FLAG[.SILENT] <flag-list>";

    private StoreCommandParser parser = new StoreCommandParser();
    
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

    private class StoreCommandParser extends CommandParser
    {
        StoreDirective storeDirective( ImapRequestLineReader request ) throws ProtocolException
        {
            int sign = 0;
            boolean silent = false;

            char next = request.nextWordChar();
            if ( next == '+' ) {
                sign = 1;
                request.consume();
            }
            else if ( next == '-' ) {
                sign = -1;
                request.consume();
            }
            else {
                sign = 0;
            }

            String directive = consumeWord( request, new NoopCharValidator() );
            if ( "FLAGS".equalsIgnoreCase( directive ) ) {
                silent = false;
            }
            else if ( "FLAGS.SILENT".equalsIgnoreCase( directive ) ) {
                silent = true;
            }
            else {
                throw new ProtocolException( "Invalid Store Directive: '" + directive + "'" );
            }
            return new StoreDirective( sign, silent );
        }
    }

    private class StoreDirective
    {
        private int sign;
        private boolean silent;

        public StoreDirective( int sign, boolean silent )
        {
            this.sign = sign;
            this.silent = silent;
        }

        public int getSign()
        {
            return sign;
        }

        public boolean isSilent()
        {
            return silent;
        }
    }

    protected AbstractImapCommandMessage decode(ImapRequestLineReader request) throws ProtocolException {
        return decode(request, false);
    }

    public AbstractImapCommandMessage decode(ImapRequestLineReader request, boolean useUids) throws ProtocolException {
        
        final IdRange[] idSet = parser.parseIdRange( request );
        final StoreDirective directive = parser.storeDirective( request );
        final Flags flags = parser.flagList( request );
        parser.endLine( request );
        return new StoreCommandMessage(idSet, directive, flags, useUids);
    }
    
    private class StoreCommandMessage extends AbstractImapCommandMessage {
        private final IdRange[] idSet;
        private final StoreDirective directive;
        private final Flags flags;
        private final boolean useUids;
        
        public StoreCommandMessage(final IdRange[] idSet, final StoreDirective directive, final Flags flags, final boolean useUids) {
            super();
            this.idSet = idSet;
            this.directive = directive;
            this.flags = flags;
            this.useUids = useUids;
        }
        
        protected ImapResponseMessage doProcess(ImapSession session) throws MailboxException, AuthorizationException, ProtocolException {

            ImapMailboxSession mailbox = session.getSelected().getMailbox();
            MailboxListener silentListener = null;

            final boolean replace;
            final boolean value;
            if (directive.getSign() < 0) {
                value=false;
                replace=false;
            }
            else if (directive.getSign() > 0) {
                value=true;
                replace=false;
            }
            else {
                replace=true;
                value=true;
            }
            try {
                if (directive.isSilent()) {
                    silentListener = session.getSelected().getMailbox();
                }
                for (int i = 0; i < idSet.length; i++) {
                    final GeneralMessageSet messageSet = GeneralMessageSetImpl
                            .range(idSet[i].getLowVal(), idSet[i].getHighVal(),
                                    useUids);

                    mailbox.setFlags(flags, value, replace, messageSet,
                            silentListener);
                }
            } catch (MailboxManagerException e) {
                throw new MailboxException(e);
            }
            
            final StoreResponseMessage result = new StoreResponseMessage(StoreCommand.this, useUids);
            return result;
        }
    }
    
    private static class StoreResponseMessage extends AbstractCommandResponseMessage {
        // TODO: use factory to improve memory usage
        private final boolean useUids;
        
        public StoreResponseMessage(ImapCommand command, final boolean useUids) {
            super(command);
            this.useUids = useUids;
        }

        void doEncode(ImapResponse response, ImapSession session, ImapCommand command) throws MailboxException {
            boolean omitExpunged = (!useUids);
            session.unsolicitedResponses( response, omitExpunged , useUids);
            response.commandComplete( command );            
        }
        
    }
}
/*
6.4.6.  STORE Command

   Arguments:  message set
               message data item name
               value for message data item

   Responses:  untagged responses: FETCH

   Result:     OK - store completed
               NO - store error: can't store that data
               BAD - command unknown or arguments invalid

      The STORE command alters data associated with a message in the
      mailbox.  Normally, STORE will return the updated value of the
      data with an untagged FETCH response.  A suffix of ".SILENT" in
      the data item name prevents the untagged FETCH, and the server
      SHOULD assume that the client has determined the updated value
      itself or does not care about the updated value.

         Note: regardless of whether or not the ".SILENT" suffix was
         used, the server SHOULD send an untagged FETCH response if a
         change to a message's flags from an external source is
         observed.  The intent is that the status of the flags is
         determinate without a race condition.

      The currently defined data items that can be stored are:

      FLAGS <flag list>
                     Replace the flags for the message with the
                     argument.  The new value of the flags are returned
                     as if a FETCH of those flags was done.

      FLAGS.SILENT <flag list>
                     Equivalent to FLAGS, but without returning a new
                     value.

      +FLAGS <flag list>
                     Add the argument to the flags for the message.  The
                     new value of the flags are returned as if a FETCH
                     of those flags was done.

      +FLAGS.SILENT <flag list>
                     Equivalent to +FLAGS, but without returning a new
                     value.

      -FLAGS <flag list>
                     Remove the argument from the flags for the message.
                     The new value of the flags are returned as if a
                     FETCH of those flags was done.

      -FLAGS.SILENT <flag list>
                     Equivalent to -FLAGS, but without returning a new
                     value.

   Example:    C: A003 STORE 2:4 +FLAGS (\Deleted)
               S: * 2 FETCH FLAGS (\Deleted \Seen)
               S: * 3 FETCH FLAGS (\Deleted)
               S: * 4 FETCH FLAGS (\Deleted \Flagged \Seen)
               S: A003 OK STORE completed

*/
