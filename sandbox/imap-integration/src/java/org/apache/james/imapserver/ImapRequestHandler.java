/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.james.imapserver.commands.CommandParser;
import org.apache.james.imapserver.commands.ImapCommand;
import org.apache.james.imapserver.commands.ImapCommandFactory;

/**
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 109034 $
 */
public final class ImapRequestHandler
{
    private ImapCommandFactory imapCommands = new ImapCommandFactory();
    private CommandParser parser = new CommandParser();
    private static final String REQUEST_SYNTAX = "Protocol Error: Was expecting <tag SPACE command [arguments]>";

    /**
     * This method parses POP3 commands read off the wire in handleConnection.
     * Actual processing of the command (possibly including additional back and
     * forth communication with the client) is delegated to one of a number of
     * command specific handler methods.  The primary purpose of this method is
     * to parse the raw command string to determine exactly which handler should
     * be called.  It returns true if expecting additional commands, false otherwise.
     *
     * @return whether additional commands are expected.
     */
    public boolean handleRequest( InputStream input,
                                  OutputStream output,
                                  ImapSession session )
            throws ProtocolException
    {
        ImapRequestLineReader request = new ImapRequestLineReader( input, output );
        try {
            request.nextChar();
        }
        catch ( ProtocolException e ) {
            return false;
        }

        ImapResponse response = new ImapResponse( output );

        doProcessRequest( request, response, session );

        // Consume the rest of the line, throwing away any extras. This allows us
        // to clean up after a protocol error.
        request.consumeLine();

        return true;
    }

    private void doProcessRequest( ImapRequestLineReader request,
                                   ImapResponse response,
                                   ImapSession session)
    {
        String tag = null;
        String commandName = null;

        try {
            tag = parser.tag( request );
        }
        catch ( ProtocolException e ) {
            response.badResponse( REQUEST_SYNTAX );
            return;
        }

//        System.out.println( "Got <tag>: " + tag );
        response.setTag( tag );
        try {
            commandName = parser.atom( request );
        }
        catch ( ProtocolException e ) {
            response.commandError( REQUEST_SYNTAX );
            return;
        }

//        System.out.println( "Got <command>: " + commandName );
        ImapCommand command = imapCommands.getCommand( commandName );
        if ( command == null )
        {
            response.commandError( "Invalid command.");
            return;
        }

        if ( !command.validForState( session.getState() ) ) {
            response.commandFailed( command, "Command not valid in this state" );
            return;
        }

        command.process( request, response, session );
    }


}
