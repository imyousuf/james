/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.AccessControlException;
import org.apache.james.AuthorizationException;
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
