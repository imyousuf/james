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

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.imapserver.AccessControlException;
import org.apache.james.util.Assert;
import org.apache.james.imapserver.*;

import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Base Class for all Commands.
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */

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

    System.out.println("CommandTemplate.process command: '"+getCommand()+"'");
        for ( Iterator iter = args.iterator(); iter.hasNext(); ) {
            System.out.println("CommandTemplate.process ARGUMENT");
            Object o =  iter.next();
            ImapArgument arg = (ImapArgument) o;
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
        System.out.println("CommandTemplate.process starting doProcess");
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
            } catch ( MailboxException me ) {
                if ( me.isRemote() ) {
                    session.noResponse( "[REFERRAL " + me.getRemoteServer() + "]" + SP + "Remote mailbox" );
                } else {
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
