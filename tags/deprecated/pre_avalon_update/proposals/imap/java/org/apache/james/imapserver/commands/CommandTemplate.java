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
import org.apache.james.util.Assert;
import org.apache.james.imapserver.*;

import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

abstract class CommandTemplate 
        extends AbstractLogEnabled implements ImapCommand, ImapConstants
{
    protected String commandName;
    private List args = new ArrayList();

    protected String getCommand()
    {
        return this.commandName;
    }

    /**
     * @return a List of <code>ImapArgument</code> objects.
     */
    protected List getArgs()
    {
        return this.args;
    }

    protected String getExpectedMessage()
    {
        StringBuffer msg = new StringBuffer( "<tag> " );
        msg.append( getCommand() );

        List args = getArgs();
        for ( Iterator iter = args.iterator(); iter.hasNext(); ) {
            msg.append( " " );
            ImapArgument arg = (ImapArgument) iter.next();
            msg.append( arg.format() );
        }
        return msg.toString();
    }

    /**
     * Template methods for handling command processing.
     */
    public boolean process( ImapRequest request, ImapSession session )
    {
        StringTokenizer tokens = request.getCommandLine();

        List args = getArgs();
        List argValues = new ArrayList();

        for ( Iterator iter = args.iterator(); iter.hasNext(); ) {
            ImapArgument arg = (ImapArgument) iter.next();
            try {
                argValues.add( arg.parse( tokens ) );
            }
            catch ( Exception e ) {
                String badMsg = e.getMessage() + ": Command should be " + getExpectedMessage();
                session.badResponse( badMsg );
                return true;
            }
        }

        if ( tokens.hasMoreTokens() ) {
            String badMsg = "Extra token found: Command should be " + getExpectedMessage();
            session.badResponse( badMsg );
            return true;
        }

        return doProcess( request, session, argValues );

    }

    protected abstract boolean doProcess( ImapRequest request, ImapSession session, List argValues );

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

    /** TODO - decode quoted strings properly, with escapes. */
    public static String readAstring( StringTokenizer tokens )
    {
        if ( ! tokens.hasMoreTokens() ) {
            throw new RuntimeException( "Not enough tokens" );
        }
        String token = tokens.nextToken();
        Assert.isTrue( token.length() > 0 );

        StringBuffer astring = new StringBuffer( token );

        if ( astring.charAt(0) == '\"' ) {
            while ( astring.length() == 1 ||
                    astring.charAt( astring.length() - 1 ) != '\"' ) {
                if ( tokens.hasMoreTokens() ) {
                    astring.append( tokens.nextToken() );
                }
                else {
                    throw new RuntimeException( "Missing closing quote" );
                }
            }
            astring.deleteCharAt( 0 );
            astring.deleteCharAt( astring.length() - 1 );
        }

        return astring.toString();
    }

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

    public void setArgs( List args )
    {
        this.args = args;
    }
}
