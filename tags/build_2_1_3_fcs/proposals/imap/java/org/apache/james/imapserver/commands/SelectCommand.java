/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

import org.apache.james.imapserver.AccessControlException;
import org.apache.james.imapserver.ACLMailbox;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;

import java.util.StringTokenizer;
import java.util.List;

class SelectCommand extends AuthenticatedSelectedStateCommand
{
    public SelectCommand()
    {
        this.commandName = "SELECT";

        this.getArgs().add( new AstringArgument( "mailbox" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.getCommand();

        // selecting a mailbox deselects current mailbox,
        // even if this select fails
        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.getCurrentMailbox().removeMailboxEventListener( session );
            session.getImapHost().releaseMailbox( session.getCurrentUser(), session.getCurrentMailbox() );
            session.setState( ImapSessionState.AUTHENTICATED );
            session.setCurrentMailbox( null );
            session.setCurrentIsReadOnly( false );
        }

        String folder = (String) argValues.get( 0 );
        ACLMailbox mailbox = getMailbox( session, folder, command );
        if ( mailbox == null ) {
            return true;
        }
        else {
            session.setCurrentMailbox( mailbox );
        }

        try { // long tries clause against an AccessControlException
            if ( !session.getCurrentMailbox().hasReadRights( session.getCurrentUser() ) ) {
                session.noResponse( command, "Read access not granted." );
                return true;
            }
            if ( command.equalsIgnoreCase( "SELECT" ) ) {
                if ( !session.getCurrentMailbox().isSelectable( session.getCurrentUser() ) ) {
                    session.noResponse( "Mailbox exists but is not selectable" );
                    return true;
                }
            }

            // Have mailbox with at least read rights. Server setup.
            session.getCurrentMailbox().addMailboxEventListener( session );
            session.setCurrentFolder( folder );
            session.setState( ImapSessionState.SELECTED );
            getLogger().debug( "Current folder for user " + session.getCurrentUser() + " from "
                               + session.getRemoteHost() + "(" + session.getRemoteIP() + ") is "
                               + session.getCurrentFolder() );

            // Inform client
            session.getOut().println( UNTAGGED + SP + "FLAGS ("
                                       + session.getCurrentMailbox().getSupportedFlags() + ")" );
            if ( !session.getCurrentMailbox().allFlags( session.getCurrentUser() ) ) {
                session.untaggedResponse( " [PERMANENTFLAGS ("
                                           + session.getCurrentMailbox().getPermanentFlags( session.getCurrentUser() )
                                           + ") ]" );
            }
            session.checkSize();
            session.getOut().println( UNTAGGED + SP + OK + " [UIDVALIDITY "
                                       + session.getCurrentMailbox().getUIDValidity() + "]" );
            int oldestUnseen = session.getCurrentMailbox().getOldestUnseen( session.getCurrentUser() );
            if ( oldestUnseen > 0 ) {
                session.getOut().println( UNTAGGED + SP + OK + " [UNSEEN "
                                           + oldestUnseen + "] " + oldestUnseen + " is the first unseen" );
            }
            else {
                session.getOut().println( UNTAGGED + SP + OK + " No unseen messages" );
            }
            session.setSequence( session.getCurrentMailbox().listUIDs( session.getCurrentUser() ));

            if ( command.equalsIgnoreCase( "EXAMINE" ) ) {
                session.setCurrentIsReadOnly( true );

                session.okResponse("[READ-ONLY] " + command );
                return true;

            }
            else if ( session.getCurrentMailbox().isReadOnly( session.getCurrentUser() ) ) {
                session.setCurrentIsReadOnly( true );
                session.okResponse( "[READ-ONLY] " + command );
                return true;
            }
            session.okResponse( "[READ-WRITE] " + command );
            return true;
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "No such mailbox." );
            session.logACE( ace );
            return true;
        }
    }
}
