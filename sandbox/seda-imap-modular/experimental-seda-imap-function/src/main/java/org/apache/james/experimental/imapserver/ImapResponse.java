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

package org.apache.james.experimental.imapserver;

import javax.mail.Flags;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.store.MessageFlags;

/**
 * Class providing methods to send response messages from the server
 * to the client.
 */
public class ImapResponse extends AbstractLogEnabled implements ImapConstants, ImapResponseWriter {
    
    public static final String FETCH = "FETCH";
    public static final String EXPUNGE = "EXPUNGE";
    public static final String RECENT = "RECENT";
    public static final String EXISTS = "EXISTS";
    public static final String FLAGS = "FLAGS";
    public static final String FAILED = "failed.";
    private final ImapResponseWriter writer;

    public ImapResponse( final ImapResponseWriter writer )
    {
        this.writer = writer;
    }

    /**
     * Writes a standard tagged OK response on completion of a command.
     * Response is writen as:
     * <pre>     a01 OK COMMAND_NAME completed.</pre>
     *
     * @param command The ImapCommand which was completed.
     */
    public void commandComplete( final ImapCommand command, final String tag )
    {
        commandComplete( command, null , tag);
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
    public void commandComplete( final ImapCommand command, final String responseCode, final String tag)
    {
        tag(tag);
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
    public void commandFailed( final ImapCommand command, final String reason , final String tag)
    {
        commandFailed( command, null, reason , tag);
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
                               String reason, 
                               final String tag)
    {
        tag(tag);
        message( NO );
        responseCode( responseCode );
        commandName( command );
        message( FAILED );
        message( reason );
        end();
        final Logger logger = getLogger();
        if (logger!= null && logger.isInfoEnabled()) {
            logger.info("COMMAND FAILED [" + responseCode + "] - " + reason);
        }
    }

    /**
     * Writes a standard BAD response on command error, together with a
     * descriptive message.
     * Response is writen as:
     * <pre>     a01 BAD <message></pre>
     *
     * @param message The descriptive error message.
     */
    public void commandError( final String message, final String tag )
    {
        tag(tag);
        message( BAD );
        message( message );
        end();
        final Logger logger = getLogger();
        if (logger != null && logger.isInfoEnabled()) {
            logger.info("ERROR - " + message); 
        }
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
        final Logger logger = getLogger(); 
        if (logger != null && logger.isInfoEnabled()) { 
            logger.info("BAD - " + message); 
        }
    }
    
    /**
     * Writes a standard untagged BAD response, together with a descriptive message.
     */
    public void badResponse( String message , String tag )
    {
    	tag(tag);
        message( BAD );
        message( message );
        end();
        final Logger logger = getLogger(); 
        if (logger != null && logger.isInfoEnabled()) { 
            logger.info("BAD - " + message); 
        }
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

    public void flagsResponse( Flags flags )
    {
        untagged();
        message( FLAGS );
        message( MessageFlags.format(flags) );
        end();
    }

    public void existsResponse( int count )
    {
        untagged();
        message( count );
        message( EXISTS );
        end();
    }

    public void recentResponse( int count )
    {
        untagged();
        message( count );
        message( RECENT );
        end();
    }

    public void expungeResponse( int msn )
    {
        untagged();
        message( msn );
        message( EXPUNGE );
        end();
    }

    public void fetchResponse( int msn, String msgData )
    {
        untagged();
        message( msn );
        message( FETCH );
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
    public void taggedResponse( String message, String tag )
    {
        tag(tag);
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
    
    public void byeResponse( String message ) {
        untaggedResponse(BYE + SP + message);
    }

    public void untagged()
    {
        writer.untagged();
    }

    private void commandName( final ImapCommand command )
    {
        final String name = command.getName();
        commandName(name);
    }

    public void commandName(final String name) {
        writer.commandName(name);
    }

    public void message( final String message )
    {
        if ( message != null ) {
            writer.message(message);
        }
    }

    public void message( final int number )
    {
        writer.message(number);
    }

    public void responseCode( final String responseCode )
    {
        if ( responseCode != null ) {
            writer.responseCode(responseCode);
        }
    }

    public void end()
    {
        writer.end();
    }

    public void permanentFlagsResponse(Flags flags) {
        untagged();
        message(OK);
        responseCode("PERMANENTFLAGS " + MessageFlags.format(flags));
        end();
    }

    public void tag(String tag) {
        writer.tag(tag);
    }
}
