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
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;

/**
 * Handles processeing for the SELECT imap command.
 *
 * @version $Revision: 109034 $
 */
class SelectCommand extends AuthenticatedStateCommand
{
    public static final String NAME = "SELECT";
    public static final String ARGS = "mailbox";
    
    // TODO: the inheritance tree seems a little wrong
    private final boolean isExamine;
    
    protected SelectCommand(boolean isExamine) {
        this.isExamine = isExamine;
    }
    
    public SelectCommand() {
        this(false);
    }

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
        final String mailboxName = parser.mailbox( request );
        parser.endLine( request );
        final SelectCommandMessage result = new SelectCommandMessage(this, mailboxName, isExamine, tag);
        return result;
    }
    
    private static class SelectCommandMessage extends AbstractImapCommandMessage {
        private final String mailboxName;
        private final boolean isExamine;
        
        public SelectCommandMessage(final ImapCommand command, final String mailboxName, final boolean isExamine,
                final String tag) {
            super(tag, command);
            this.mailboxName = mailboxName;
            this.isExamine = isExamine;
        }

        protected ImapResponseMessage doProcess(ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
            ImapResponseMessage result;
            session.deselect();
            try {
                String fullMailboxName=session.buildFullName(this.mailboxName);
                selectMailbox(fullMailboxName, session, isExamine);
                ImapMailboxSession mailbox = session.getSelected().getMailbox();
                final Flags permanentFlags = mailbox.getPermanentFlags();
                final boolean writeable = mailbox.isWriteable();
                final boolean resetRecent = !isExamine;
                final int recentCount = mailbox.getRecentCount(resetRecent);
                final long uidValidity = mailbox.getUidValidity();
                final MessageResult firstUnseen = mailbox.getFirstUnseen(MessageResult.MSN);
                final int messageCount = mailbox.getMessageCount();
                result = new SelectResponseMessage(command, permanentFlags, 
                        writeable, recentCount, uidValidity, firstUnseen, messageCount,
                        tag);
            } catch (MailboxManagerException e) {
                throw new MailboxException(e);
            }
            return result;
        }
        
        private boolean selectMailbox(String mailboxName, ImapSession session, boolean readOnly) throws MailboxException, MailboxManagerException {
            ImapMailboxSession mailbox = session.getMailboxManager().getImapMailboxSession(mailboxName);

            if ( !mailbox.isSelectable() ) {
                throw new MailboxException( "Nonselectable mailbox." );
            }

            session.setSelected( mailbox, readOnly );
            return readOnly;
        }
    }
    
    private static class SelectResponseMessage extends AbstractCommandResponseMessage {
        private final Flags permanentFlags;
        private final boolean writeable ;
        private final int recentCount;
        private final long uidValidity;
        private final MessageResult firstUnseen;
        private final int messageCount;

        public SelectResponseMessage(ImapCommand command, final Flags permanentFlags,
                final boolean writeable, final int recentCount, 
                final long uidValidity, final MessageResult firstUnseen,
                final int messageCount, final String tag) {
            super(command, tag);
            this.permanentFlags = permanentFlags;
            this.writeable = writeable;
            this.recentCount = recentCount;
            this.uidValidity = uidValidity;
            this.firstUnseen = firstUnseen;
            this.messageCount = messageCount;
        }        
        
        void doEncode(ImapResponse response, ImapSession session, ImapCommand command, String tag) throws MailboxException {
            response.flagsResponse(permanentFlags);
            response.recentResponse(recentCount);
            response.okResponse("UIDVALIDITY " + uidValidity, null);
            response.existsResponse(messageCount);
            if (firstUnseen != null) {
                response.okResponse("UNSEEN " + firstUnseen.getMsn(), "Message "
                        + firstUnseen.getMsn() + " is the first unseen");
            } else {
                response.okResponse(null, "No messages unseen");
            }
            response.permanentFlagsResponse(permanentFlags);
            if (!writeable) {
                response.commandComplete(command, "READ-ONLY", tag);
            } else {
                response.commandComplete(command, "READ-WRITE", tag);
            }
        }
    }
}

/*
6.3.1.  SELECT Command

   Arguments:  mailbox name

   Responses:  REQUIRED untagged responses: FLAGS, EXISTS, RECENT
               OPTIONAL OK untagged responses: UNSEEN, PERMANENTFLAGS

   Result:     OK - select completed, now in selected state
               NO - select failure, now in authenticated state: no
                    such mailbox, can't access mailbox
               BAD - command unknown or arguments invalid

   The SELECT command selects a mailbox so that messages in the
   mailbox can be accessed.  Before returning an OK to the client,
   the server MUST send the following untagged data to the client:

      FLAGS       Defined flags in the mailbox.  See the description
                  of the FLAGS response for more detail.

      <n> EXISTS  The number of messages in the mailbox.  See the
                  description of the EXISTS response for more detail.

      <n> RECENT  The number of messages with the \Recent flag set.
                  See the description of the RECENT response for more
                  detail.

      OK [UIDVALIDITY <n>]
                  The unique identifier validity value.  See the
                  description of the UID command for more detail.

   to define the initial state of the mailbox at the client.

   The server SHOULD also send an UNSEEN response code in an OK
   untagged response, indicating the message sequence number of the
   first unseen message in the mailbox.

   If the client can not change the permanent state of one or more of
   the flags listed in the FLAGS untagged response, the server SHOULD
   send a PERMANENTFLAGS response code in an OK untagged response,
   listing the flags that the client can change permanently.

   Only one mailbox can be selected at a time in a connection;
   simultaneous access to multiple mailboxes requires multiple
   connections.  The SELECT command automatically deselects any
   currently selected mailbox before attempting the new selection.
   Consequently, if a mailbox is selected and a SELECT command that
   fails is attempted, no mailbox is selected.




Crispin                     Standards Track                    [Page 23]

RFC 2060                       IMAP4rev1                   December 1996


   If the client is permitted to modify the mailbox, the server
   SHOULD prefix the text of the tagged OK response with the
         "[READ-WRITE]" response code.

      If the client is not permitted to modify the mailbox but is
      permitted read access, the mailbox is selected as read-only, and
      the server MUST prefix the text of the tagged OK response to
      SELECT with the "[READ-ONLY]" response code.  Read-only access
      through SELECT differs from the EXAMINE command in that certain
      read-only mailboxes MAY permit the change of permanent state on a
      per-user (as opposed to global) basis.  Netnews messages marked in
      a server-based .newsrc file are an example of such per-user
      permanent state that can be modified with read-only mailboxes.

   Example:    C: A142 SELECT INBOX
               S: * 172 EXISTS
               S: * 1 RECENT
               S: * OK [UNSEEN 12] Message 12 is first unseen
               S: * OK [UIDVALIDITY 3857529045] UIDs valid
               S: * FLAGS (\Answered \Flagged \Deleted \Seen \Draft)
               S: * OK [PERMANENTFLAGS (\Deleted \Seen \*)] Limited
               S: A142 OK [READ-WRITE] SELECT completed
*/
