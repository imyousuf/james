/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.ProtocolException;

/**
 * Handles processeing for the CREATE imap command.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.3.2.1 $
 */
class CreateCommand extends AuthenticatedStateCommand
{
    public static final String NAME = "CREATE";
    public static final String ARGS = "<mailbox>";

    /** @see CommandTemplate#doProcess */
    protected void doProcess( ImapRequestLineReader request,
                              ImapResponse response,
                              ImapSession session )
            throws ProtocolException, MailboxException, AuthorizationException
    {
        String mailboxName = parser.mailbox( request );
        parser.endLine( request );

        session.getHost().createMailbox( session.getUser(), mailboxName );
        session.unsolicitedResponses( response );
        response.commandComplete( this );
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

}

/*
6.3.3.  CREATE Command

   Arguments:  mailbox name

   Responses:  no specific responses for this command

   Result:     OK - create completed
               NO - create failure: can't create mailbox with that name
               BAD - command unknown or arguments invalid

      The CREATE command creates a mailbox with the given name.  An OK
      response is returned only if a new mailbox with that name has been
      created.  It is an error to attempt to create INBOX or a mailbox
      with a name that refers to an extant mailbox.  Any error in
      creation will return a tagged NO response.

      If the mailbox name is suffixed with the server's hierarchy
      separator character (as returned from the server by a LIST
      command), this is a declaration that the client intends to create
      mailbox names under this name in the hierarchy.  Server
      implementations that do not require this declaration MUST ignore
      it.

      If the server's hierarchy separator character appears elsewhere in
      the name, the server SHOULD create any superior hierarchical names
      that are needed for the CREATE command to complete successfully.
      In other words, an attempt to create "foo/bar/zap" on a server in
      which "/" is the hierarchy separator character SHOULD create foo/
      and foo/bar/ if they do not already exist.

      If a new mailbox is created with the same name as a mailbox which
      was deleted, its unique identifiers MUST be greater than any
      unique identifiers used in the previous incarnation of the mailbox
      UNLESS the new incarnation has a different unique identifier
      validity value.  See the description of the UID command for more
      detail.

   Example:    C: A003 CREATE owatagusiam/
               S: A003 OK CREATE completed
               C: A004 CREATE owatagusiam/blurdybloop
               S: A004 OK CREATE completed

      Note: the interpretation of this example depends on whether "/"
      was returned as the hierarchy separator from LIST.  If "/" is the
      hierarchy separator, a new level of hierarchy named "owatagusiam"
      with a member called "blurdybloop" is created.  Otherwise, two
      mailboxes at the same hierarchy level are created.
*/
