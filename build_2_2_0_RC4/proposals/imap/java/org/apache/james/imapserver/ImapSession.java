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

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.imapserver.AccessControlException;
import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.commands.ImapCommand;
import org.apache.james.services.UsersRepository;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.List;

public interface ImapSession extends MailboxEventListener
{
    void okResponse( String command );

    void noResponse( String command );

    void noResponse( String command, String msg );

    void badResponse( String badMsg );

    void notImplementedResponse( String command );

    void taggedResponse( String msg );

    void untaggedResponse( String msg );

    ImapSessionState getState();

    void setState( ImapSessionState state );

    BufferedReader getIn();

    void setIn( BufferedReader in );

    PrintWriter getOut();

    void setOut( PrintWriter out );
    
    void setCanParseCommand(boolean canParseCommand);
    
    boolean getCanParseCommand();

    void checkSize();
    
    void checkExpunge();
    
    String getRemoteHost();
    
    String getRemoteIP();
    
    Logger getSecurityLogger();
    
    UsersRepository getUsers();
    
    Host getImapHost();
    
    IMAPSystem getImapSystem();
    
    String getCurrentNamespace();
    void setCurrentNamespace( String currentNamespace );
    String getCurrentSeperator();
    void setCurrentSeperator( String currentSeperator );
    String getCurrentFolder();
    void setCurrentFolder( String currentFolder );
    ACLMailbox getCurrentMailbox();
    void setCurrentMailbox( ACLMailbox currentMailbox );
    boolean isCurrentIsReadOnly();
    void setCurrentIsReadOnly( boolean currentIsReadOnly );
    boolean isConnectionClosed();
    void setConnectionClosed( boolean connectionClosed );
    String getCurrentUser();
    void setCurrentUser( String user );
    void setSequence( List sequence );

    ImapCommand getImapCommand( String command );
    
    boolean closeConnection(int exitStatus,
                                     String message1,
                                     String message2 );
    
    void logACE( AccessControlException ace );
    void logAZE( AuthorizationException aze );
    
//    ACLMailbox getBox( String user, String mailboxName );
    
    List decodeSet( String rawSet, int exists ) throws IllegalArgumentException;
}
