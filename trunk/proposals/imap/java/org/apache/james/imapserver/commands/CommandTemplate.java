/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.AccessControlException;
import org.apache.james.imapserver.*;

abstract class CommandTemplate 
        extends AbstractLogEnabled implements ImapCommand, ImapConstants
{
    /**
     * By default, valid in any state (unless overridden by subclass.
     */ 
    public boolean validForState( ImapSessionState state )
    {
        return true;
    }

    protected void logCommand( ImapRequest request, ImapSession session )
    {
        getLogger().debug( request.getCommand() + " command completed for " + 
                           session.getRemoteHost() + "(" + 
                           session.getRemoteIP() + ")" );
    }

    protected ACLMailbox getMailbox( ImapSession session, String mailboxName, String command )
    {
        if ( session.getState() == ImapSessionState.SELECTED && session.getCurrentFolder().equals( mailboxName ) ) {
            return session.getCurrentMailbox();
        }
        else {
            try {
                return session.getImapHost().getMailbox( session.getCurrentUser(), mailboxName );
            }
            catch ( MailboxException me ) {
                if ( me.isRemote() ) {
                    session.noResponse( "[REFERRAL " + me.getRemoteServer() + "]" + SP + "Remote mailbox" );
                }
                else {
                    session.noResponse( command, "Unknown mailbox" );
                    getLogger().info( "MailboxException in method getBox for user: "
                                      + session.getCurrentUser() + " mailboxName: " + mailboxName + " was "
                                      + me.getMessage() );
                }
                return null;
            }
            catch ( AccessControlException e ) {
                session.noResponse( command, "Unknown mailbox" );
                return null;
            }
        }
    }

    /**
     * Returns a full mailbox name, resolving the supplied name against the current location.
     */
    public String decodeMailboxName( String name )
    {
        getLogger().debug( "Method decodeMailboxName called for " + name );
        return decodeAstring( name );
    }

    /** TODO - decode quoted strings properly, with escapes. */
    public String decodeAstring( String rawAstring )
    {
        if ( rawAstring.length() == 0 ) {
            return rawAstring;
        }

        if ( rawAstring.startsWith( "\"" ) ) {
            //quoted string
            if ( rawAstring.endsWith( "\"" ) ) {
                if ( rawAstring.length() == 2 ) {
                    return new String(); //ie blank
                }
                else {
                    return rawAstring.substring( 1, rawAstring.length() - 1 );
                }
            }
            else {
                getLogger().error( "Quoted string with no closing quote." );
                return null;
            }
        }
        else {
            //atom
            return rawAstring;
        }
    }

}
