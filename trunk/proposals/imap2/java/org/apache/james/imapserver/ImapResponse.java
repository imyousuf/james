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

package org.apache.james.imapserver;

import org.apache.james.imapserver.commands.ImapCommand;
import org.apache.james.imapserver.store.MessageFlags;
import org.apache.james.util.InternetPrintWriter;

import java.io.PrintWriter;
import java.io.OutputStream;

/**
 * Class providing methods to send response messages from the server
 * to the client.
 */
public class ImapResponse implements ImapConstants
{
    private PrintWriter writer;
    private String tag = UNTAGGED;

    public ImapResponse( OutputStream output )
    {
        this.writer = new InternetPrintWriter( output, true );
    }

    public void setTag( String tag )
    {
        this.tag = tag;
    }

    /**
     * Writes a standard tagged OK response on completion of a command.
     * Response is writen as:
     * <pre>     a01 OK COMMAND_NAME completed.</pre>
     *
     * @param command The ImapCommand which was completed.
     */
    public void commandComplete( ImapCommand command )
    {
        commandComplete( command, null );
    }

    /**
     * Writes a standard tagged OK response on completion of a command,
     * with a response code (eg READ-WRITE)
     * Response is writen as:
     * <pre>     a01 OK [responseCode] COMMAND_NAME completed.</pre>
     *
     * @param command The ImapCommand which was completed.
     * @param responseCode A string response code to send to the client.
     */
    public void commandComplete( ImapCommand command, String responseCode )
    {
        tag();
        message( OK );
        responseCode( responseCode );
        commandName( command );
        message( "completed." );
        end();
    }

    /**
     * Writes a standard NO response on command failure, together with a
     * descriptive message.
     * Response is writen as:
     * <pre>     a01 NO COMMAND_NAME failed. <reason></pre>
     *
     * @param command The ImapCommand which failed.
     * @param reason A message describing why the command failed.
     */
    public void commandFailed( ImapCommand command, String reason )
    {
        commandFailed( command, null, reason );
    }

    /**
     * Writes a standard NO response on command failure, together with a
     * descriptive message.
     * Response is writen as:
     * <pre>     a01 NO [responseCode] COMMAND_NAME failed. <reason></pre>
     *
     * @param command The ImapCommand which failed.
     * @param responseCode The Imap response code to send.
     * @param reason A message describing why the command failed.
     */
    public void commandFailed( ImapCommand command,
                               String responseCode,
                               String reason )
    {
        tag();
        message( NO );
        responseCode( responseCode );
        commandName( command );
        message( "failed." );
        message( reason );
        end();
    }

    /**
     * Writes a standard BAD response on command error, together with a
     * descriptive message.
     * Response is writen as:
     * <pre>     a01 BAD <message></pre>
     *
     * @param message The descriptive error message.
     */
    public void commandError( String message )
    {
        tag();
        message( BAD );
        message( message );
        end();
    }

    /**
     * Writes a standard untagged BAD response, together with a descriptive message.
     */
    public void badResponse( String message )
    {
        untagged();
        message( BAD );
        message( message );
        end();
    }

    /**
     * Writes an untagged OK response, with the supplied response code,
     * and an optional message.
     * @param responseCode The response code, included in [].
     * @param message The message to follow the []
     */
    public void okResponse( String responseCode, String message )
    {
        untagged();
        message( OK );
        responseCode( responseCode );
        message( message );
        end();
    }

    public void flagsResponse( MessageFlags flags )
    {
        untagged();
        message( "FLAGS" );
        message( flags.format() );
        end();
    }

    public void existsResponse( int count )
    {
        untagged();
        message( count );
        message( "EXISTS" );
        end();
    }

    public void recentResponse( int count )
    {
        untagged();
        message( count );
        message( "RECENT" );
        end();
    }

    public void expungeResponse( int msn )
    {
        untagged();
        message( msn );
        message( "EXPUNGE" );
        end();
    }

    public void fetchResponse( int msn, String msgData )
    {
        untagged();
        message( msn );
        message( "FETCH" );
        message( "(" + msgData + ")" );
        end();
    }

    public void commandResponse( ImapCommand command, String message )
    {
        untagged();
        commandName( command );
        message( message );
        end();
    }

    /**
     * Writes the message provided to the client, prepended with the
     * request tag.
     *
     * @param message The message to write to the client.
     */
    public void taggedResponse( String message )
    {
        tag();
        message( message );
        end();
    }

    /**
     * Writes the message provided to the client, prepended with the
     * untagged marker "*".
     *
     * @param message The message to write to the client.
     */
    public void untaggedResponse( String message )
    {
        untagged();
        message( message );
        end();
    }

    private void untagged()
    {
        writer.print( UNTAGGED );
    }

    private void tag()
    {
        writer.print( tag );
    }

    private void commandName( ImapCommand command )
    {
        String name = command.getName();
        writer.print( SP );
        writer.print( name );
    }

    private void message( String message )
    {
        if ( message != null ) {
            writer.print( SP );
            writer.print( message );
        }
    }

    private void message( int number )
    {
        writer.print( SP );
        writer.print( number );
    }

    private void responseCode( String responseCode )
    {
        if ( responseCode != null ) {
            writer.print( " [" );
            writer.print( responseCode );
            writer.print( "]" );
        }
    }

    private void end()
    {
        writer.println();
        writer.flush();
    }

    public void permanentFlagsResponse(MessageFlags flags) {
        untagged();
        message(OK);
        responseCode("PERMANENTFLAGS " + flags.format());
        end();
    }
}
