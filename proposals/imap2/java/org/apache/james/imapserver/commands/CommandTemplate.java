/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.ImapConstants;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.ImapRequestParser;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.ProtocolException;

/**
 * Base class for all command implementations. This class provides common
 * core functionality useful for all {@link org.apache.james.imapserver.commands.ImapCommand} implementations.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
abstract class CommandTemplate
        extends AbstractLogEnabled
        implements ImapCommand, ImapConstants
{
    /**
     * By default, valid in any state (unless overridden by subclass.
     * @see org.apache.james.imapserver.commands.ImapCommand#validForState
     */
    public boolean validForState( ImapSessionState state )
    {
        return true;
    }

    /**
     * Template methods for handling command processing. This method reads
     * argument values (validating them), and checks the request for correctness.
     * If correct, the command processing is delegated to the specific command
     * implemenation.
     *
     * @see ImapCommand#process
     */
    public void process( ImapRequestParser request,
                         ImapResponse response,
                         ImapSession session )
    {
        try {
            doProcess( request, response, session );
        }
        catch ( MailboxException e ) {
            response.commandFailed( this, e.getMessage() );
        }
        catch ( AuthorizationException e ) {
            String msg = "Authorization error: Lacking permissions to perform requested operation.";
            response.commandFailed( this, msg );
        }
        catch ( ProtocolException e ) {
            String msg = e.getMessage() + " Command should be '" +
                    getExpectedMessage() + "'";
            response.commandError( msg );
        }
    }

    /**
     * This is the method overridden by specific command implementations to
     * perform commend-specific processing.
     *
     * @param request The client request
     * @param response The server response
     * @param session The current client session
     */
    protected abstract void doProcess( ImapRequestParser request,
                                       ImapResponse response,
                                       ImapSession session )
            throws ProtocolException, MailboxException, AuthorizationException;

    /**
     * Provides a message which describes the expected format and arguments
     * for this command. This is used to provide user feedback when a command
     * request is malformed.
     *
     * @return A message describing the command protocol format.
     */
    protected String getExpectedMessage()
    {
        StringBuffer syntax = new StringBuffer( "<tag> " );
        syntax.append( getName() );

        String args = getArgSyntax();
        if ( args != null && args.length() > 0 ) {
            syntax.append( " " );
            syntax.append( args );
        }

        return syntax.toString();
    }

    /**
     * Provides the syntax for the command arguments if any. This value is used
     * to provide user feedback in the case of a malformed request.
     *
     * For commands which do not allow any arguments, <code>null</code> should
     * be returned.
     *
     * @return The syntax for the command arguments, or <code>null</code> for
     *         commands without arguments.
     */
    protected abstract String getArgSyntax();

    protected ImapMailbox getMailbox( String mailboxName,
                                      ImapSession session,
                                      boolean mustExist )
            throws MailboxException
    {
        return session.getHost().getMailbox( session.getUser(), mailboxName, mustExist );
    }

//    /**
//     * Logs the command details.
//     */
//    protected void logCommand( ImapRequestParser request, ImapSession session )
//    {
//        getLogger().debug( request.getCommand() + " command completed for " +
//                           session.getRemoteHost() + "(" +
//                           session.getRemoteIP() + ")" );
//    }
//
//    protected ACLMailbox getMailbox( ImapSession session, String mailboxName, String command )
//    {
//        if ( session.getState() == ImapSessionState.SELECTED && session.currentMailbox().equals( mailboxName ) ) {
//            return session.getCurrentMailbox();
//        }
//        else {
//            try {
//                return session.getImapHost().getMailbox( session.getCurrentUser(), mailboxName );
//            } catch ( MailboxException me ) {
//                if ( me.isRemote() ) {
//                    session.noResponse( "[REFERRAL " + me.getRemoteServer() + "]" + SP + "Remote mailbox" );
//                } else {
//                    session.noResponse( command, "Unknown mailbox" );
//                    getLogger().info( "MailboxException in method getBox for user: "
//                                      + session.getCurrentUser() + " mailboxName: " + mailboxName + " was "
//                                      + me.getMessage() );
//                }
//                return null;
//            }
//            catch ( AccessControlException e ) {
//                session.noResponse( command, "Unknown mailbox" );
//                return null;
//            }
//        }
//    }

//    public static String readAstring( StringTokenizer tokens )
//    {
//        if ( ! tokens.hasMoreTokens() ) {
//            throw new RuntimeException( "Not enough tokens" );
//        }
//        String token = tokens.nextToken();
//        Assert.isTrue( token.length() > 0 );
//
//        StringBuffer astring = new StringBuffer( token );
//
//        if ( astring.charAt(0) == '\"' ) {
//            while ( astring.length() == 1 ||
//                    astring.charAt( astring.length() - 1 ) != '\"' ) {
//                if ( tokens.hasMoreTokens() ) {
//                    astring.append( tokens.nextToken() );
//                }
//                else {
//                    throw new RuntimeException( "Missing closing quote" );
//                }
//            }
//            astring.deleteCharAt( 0 );
//            astring.deleteCharAt( astring.length() - 1 );
//        }
//
//        return astring.toString();
//    }
//
//    public String decodeAstring( String rawAstring )
//    {
//
//        if ( rawAstring.length() == 0 ) {
//            return rawAstring;
//        }
//
//        if ( rawAstring.startsWith( "\"" ) ) {
//            //quoted string
//            if ( rawAstring.endsWith( "\"" ) ) {
//                if ( rawAstring.length() == 2 ) {
//                    return new String(); //ie blank
//                }
//                else {
//                    return rawAstring.substring( 1, rawAstring.length() - 1 );
//                }
//            }
//            else {
//                getLogger().error( "Quoted string with no closing quote." );
//                return null;
//            }
//        }
//        else {
//            //atom
//            return rawAstring;
//        }
//    }
//
//    public void setArgs( List args )
//    {
//        this.args = args;
//    }
}
