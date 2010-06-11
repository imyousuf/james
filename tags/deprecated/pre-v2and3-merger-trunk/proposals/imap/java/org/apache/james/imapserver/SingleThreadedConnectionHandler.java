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

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.imapserver.AccessControlException;
import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.Constants;
import org.apache.james.imapserver.commands.ImapCommand;
import org.apache.james.imapserver.commands.ImapCommandFactory;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.util.InternetPrintWriter;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.StringTokenizer;

/**
 * An IMAP Handler handles one IMAP connection. TBC - it may spawn worker
 * threads someday.
 *
 * <p> Based on SMTPHandler and POP3Handler by Federico Barbieri <scoobie@systemy.it>
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.3 on 08 Aug 2002
 */
public class SingleThreadedConnectionHandler
        extends BaseCommand
        implements ConnectionHandler, Composable, Configurable,
        Initializable, Disposable, Target, MailboxEventListener,
        ImapSession, ImapConstants
{

    private Logger securityLogger;
    private MailServer mailServer;
    private UsersRepository users;
    private TimeScheduler scheduler;

    private ImapSession _session;
    private MailboxEventListener _mailboxListener;

    private ImapCommandFactory _imapCommands;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private OutputStream outs;
    private String remoteHost;
    private String remoteIP;
    private String softwaretype = "JAMES IMAP4rev1 Server " + Constants.SOFTWARE_VERSION;
    private ImapSessionState state;
    private String user;

    private IMAPSystem imapSystem;
    private Host imapHost;
    private String namespaceToken;
    private String currentNamespace = null;
    private String currentSeperator = null;
    private String commandRaw;
    
    //currentFolder holds the client-dependent absolute address of the current
    //folder, that is current Namespace and full mailbox hierarchy.
    private String currentFolder = null;
    private ACLMailbox currentMailbox = null;
    private boolean currentIsReadOnly = false;
    private boolean connectionClosed = false;
    private String tag;
    private boolean checkMailboxFlag = false;
    private int exists;
    private int recent;
    private List sequence;
    
    private boolean canParseCommand = true;
    
    public SingleThreadedConnectionHandler()
    {
        _session = this;
        _mailboxListener = this;

        _imapCommands = new ImapCommandFactory();
    }

    /**
     * Set the components logger.
     *
     * @param logger the logger
     */
    public void enableLogging( Logger logger )
    {
        super.enableLogging( logger );
        _imapCommands.enableLogging( logger );
    }

    public void compose( final ComponentManager componentManager )
            throws ComponentException
    {

        mailServer = (MailServer) componentManager.
                lookup( "org.apache.james.services.MailServer" );
        UsersStore usersStore = (UsersStore) componentManager.
                lookup( "org.apache.james.services.UsersStore" );
        users = usersStore.getRepository( "LocalUsers" );
        scheduler = (TimeScheduler) componentManager.
                lookup( "org.apache.avalon.cornerstone.services.scheduler.TimeScheduler" );
        imapSystem = (IMAPSystem) componentManager.
                lookup( "org.apache.james.imapserver.IMAPSystem" );
        imapHost = (Host) componentManager.
                lookup( "org.apache.james.imapserver.Host" );
    }

    public void initialize() throws Exception
    {
        getLogger().info( "SingleThreadedConnectionHandler starting ..." );
        securityLogger = getLogger().getChildLogger( "security" );
        getLogger().info( "SingleThreadedConnectionHandler initialized" );
    }

    /**
     * Handle a connection.
     * This handler is responsible for processing connections as they occur.
     *
     * @param connection the connection
     * @exception IOException if an error reading from socket occurs
     * @exception ProtocolException if an error handling connection occurs
     */
    public void handleConnection( final Socket connection )
            throws IOException
    {

        try {
            this.socket = connection;
            setIn( new BufferedReader( new
                    InputStreamReader( socket.getInputStream() ) ) );
            outs = socket.getOutputStream();
            setOut( new InternetPrintWriter( outs, true ) );
            remoteHost = socket.getInetAddress().getHostName();
            remoteIP = socket.getInetAddress().getHostAddress();
        }
        catch ( Exception e ) {
            getLogger().error( "Cannot open connection from " + getRemoteHost() + " ("
                               + getRemoteIP() + "): " + e.getMessage() );
        }
        getLogger().info( "Connection from " + getRemoteHost() + " (" + getRemoteIP() + ")" );

        try {
            final PeriodicTimeTrigger trigger = new PeriodicTimeTrigger( timeout, -1 );
            scheduler.addTrigger( this.toString(), trigger, this );

            if ( false ) { // arbitrary rejection of connection
                // could screen connections by IP or host or implement
                // connection pool management
                setConnectionClosed( closeConnection( UNTAGGED_BYE,
                                                      " connection rejected.",
                                                      "" ) );
            }
            else {
                if ( false ) { // connection is pre-authenticated
                    untaggedResponse( "PREAUTH" + SP + VERSION + SP
                                      + "server" + SP + this.helloName + SP
                                      + "logged in as" + SP + _session.getCurrentUser() );
                    _session.setState( ImapSessionState.AUTHENTICATED );
                    _session.setCurrentUser( "preauth user" );
                    getSecurityLogger().info( "Pre-authenticated connection from  "
                                              + getRemoteHost() + "(" + getRemoteIP()
                                              + ") received by SingleThreadedConnectionHandler" );
                }
                else {
                    _session.getOut().println( UNTAGGED + SP + OK + SP + VERSION + SP
                                               + "Server " + this.helloName + SP + "ready" );
                    _session.setState( ImapSessionState.NON_AUTHENTICATED );
                    _session.setCurrentUser( "unknown" );
                    getSecurityLogger().info( "Non-authenticated connection from  "
                                              + getRemoteHost() + "(" + getRemoteIP()
                                              + ") received by SingleThreadedConnectionHandler" );
                }
                
                while ( true ) {
                    if (this.getCanParseCommand()) {
                       if(! parseCommand( in.readLine())) break;
                    }
                    scheduler.resetTrigger( this.toString() );
                }
            }

            if ( !isConnectionClosed() ) {
                setConnectionClosed( closeConnection( UNTAGGED_BYE,
                                                      "Server error, closing connection", "" ) );
            }

        }
        catch ( Exception e ) {
            // This should never happen once code is debugged
            getLogger().error( "Exception during connection from " + getRemoteHost()
                               + " (" + getRemoteIP() + ") : " + e.getMessage() );
            e.printStackTrace();
            setConnectionClosed( closeConnection( UNTAGGED_BYE,
                                                  "Error processing command.", "" ) );
        }

        scheduler.removeTrigger( this.toString() );
    }

    public void targetTriggered( final String triggerName )
    {
        getLogger().info( "Connection timeout on socket" );
        setConnectionClosed( closeConnection( UNTAGGED_BYE,
                                              "Autologout. Idle too long.", "" ) );
    }

    public boolean closeConnection( int exitStatus,
                                    String message1,
                                    String message2 )
    {
        scheduler.removeTrigger( this.toString() );
        if ( _session.getState() == ImapSessionState.SELECTED ) {
            getCurrentMailbox().removeMailboxEventListener( this );
            getImapHost().releaseMailbox( _session.getCurrentUser(), getCurrentMailbox() );
        }

        try {
            switch ( exitStatus ) {
                case 0:
                    untaggedResponse( "BYE" + SP + "Server logging out" );
                    okResponse( "LOGOUT" );
                    break;
                case 1:
                    untaggedResponse( "BYE" + SP + message1 );
                    okResponse( message2 );
                    break;
                case 2:
                    untaggedResponse( "BYE" + SP + message1 );
                    break;
                case 3:
                    noResponse( message1 );
                    break;
                case 4:
                    untaggedResponse( "BYE" + SP + message1 );
                    noResponse( message2 );
                    break;
            }
            _session.getOut().flush();
            socket.close();
            getLogger().info( "Connection closed" + SP + exitStatus + SP + message1
                              + SP + message2 );
        }
        catch ( IOException ioe ) {
            getLogger().error( "Exception while closing connection from " + getRemoteHost()
                               + " (" + getRemoteIP() + ") : " + ioe.getMessage() );
            try {
                socket.close();
            }
            catch ( IOException ioe2 ) {
            }
        }
        return true;
    }

    private boolean parseCommand( String next )
    {
        commandRaw = next;
        String folder = null;
        String command = null;
        boolean subscribeOnly = false;
        System.out.println("PARSING COMMAND FROM CILENT: "+next);
        if ( commandRaw == null ) return false;
        StringTokenizer commandLine = new StringTokenizer( commandRaw.trim(), " " );
        int arguments = commandLine.countTokens();
        if ( arguments == 0 ) {
            return true;
        }else {
            tag = commandLine.nextToken();
            if ( tag.length() > 10 ) {
                // this stops overlong junk.
                // Should do more validation
                badResponse( "tag too long" );
                return true;
            }
        }
        if ( arguments > 1 ) {
            command = commandLine.nextToken();
            if ( command.length() > 13 ) {// this stops overlong junk.
                // we could validate the command contents,
                // but may not be worth it
                badResponse( "overlong command attempted" );
                return true;
            }
        } else {
            badResponse( "no command sent" );
            return true;
        }
        
        // Create ImapRequestImpl object here - is this the right stage?
        ImapRequestImpl request = new ImapRequestImpl( this, command );
        request.setCommandLine( commandLine );
        request.setUseUIDs( false );
        request.setCurrentMailbox( getCurrentMailbox() );
        request.setCommandRaw( commandRaw );
        request.setTag( tag );
        request.setCurrentFolder( getCurrentFolder() );

        // At this stage we have a tag and a string which may be a command
        // Start with commands that are valid in any state
        // CAPABILITY, NOOP, LOGOUT
        
        // Commands only valid in NON_AUTHENTICATED state
        // AUTHENTICATE, LOGIN


        // Commands valid in both Authenticated and Selected states
        // NAMESPACE, GETACL, SETACL, DELETEACL, LISTRIGHTS, MYRIGHTS, SELECT
        
        // Commands valid only in Authenticated State
        // None

        // Commands valid only in Selected state
        // CHECK CLOSE COPY EXPUNGE FETCH STORE UID
        
        ImapCommand cmd = getImapCommand( command );
        
        if ( ! cmd.validForState( state ) ) {
            badResponse( command + " not valid in this state" );
            return true;
        }
        return cmd.process( request, this );
    }

    public ImapCommand getImapCommand( String command )
    {
        return _imapCommands.getCommand( command );
    }

    private void invalidStateResponse( String command )
    {
        badResponse( command + " not valid in this state" );
    }

    public void okResponse( String command )
    {
        taggedResponse( OK + SP + command + " completed" );
    }

    public void noResponse( String command )
    {
        noResponse( command, "failed" );
    }

    public void noResponse( String command, String msg )
    {
        taggedResponse( NO + SP + command + SP + msg );
    }

    public void badResponse( String badMsg )
    {
        taggedResponse( BAD + SP + badMsg );
    }

    public void notImplementedResponse( String command )
    {
        badResponse( command + " not implemented." );
    }

    public void taggedResponse( String msg )
    {
        _session.getOut().println( tag + SP + msg );
    }

    public void untaggedResponse( String msg )
    {
        _session.getOut().println( UNTAGGED + SP + msg );
    }

    public void dispose()
    {
        // todo
        getLogger().error( "Stop IMAPHandler" );
    }

    public void receiveEvent( MailboxEvent me )
    {
        if ( _session.getState() == ImapSessionState.SELECTED ) {
            checkMailboxFlag = true;
        }
    }

//    public ACLMailbox getBox( String user, String mailboxName ) throws MailboxException, AccessControlException
//    {
//        return
//        ACLMailbox tempMailbox = null;
//        try {
//            tempMailbox = getImapHost().getMailbox( user, mailboxName );
//        }
//        catch ( MailboxException me ) {
//            if ( me.isRemote() ) {
//                _session.getOut().println( tag + SP + NO + SP + "[REFERRAL " + me.getRemoteServer() + "]" + SP + "Remote mailbox" );
//            }
//            else {
//                _session.noResponse(
//                _session.getOut().println( tag + SP + NO + SP + "Unknown mailbox" );
//                getLogger().info( "MailboxException in method getBox for user: "
//                                  + user + " mailboxName: " + mailboxName + " was "
//                                  + me.getMessage() );
//            }
//
//        }
//        catch ( AccessControlException e ) {
//            _session.getOut().println( tag + SP + NO + SP + "Unknown mailbox" );
//        }
//        return tempMailbox;
//    }

    public void logACE( AccessControlException ace )
    {
        getSecurityLogger().error( "AccessControlException by user " + _session.getCurrentUser()
                                   + " from " + getRemoteHost() + "(" + getRemoteIP()
                                   + ") with " + commandRaw + " was "
                                   + ace.getMessage() );
    }

    public void logAZE( AuthorizationException aze )
    {
        getSecurityLogger().error( "AuthorizationException by user " + _session.getCurrentUser()
                                   + " from " + getRemoteHost() + "(" + getRemoteIP()
                                   + ") with " + commandRaw + " was "
                                   + aze.getMessage() );
    }

    public PrintWriter getPrintWriter()
    {
        return _session.getOut();
    }

    public OutputStream getOutputStream()
    {
        return outs;
    }

    public String getUser()
    {
        return _session.getCurrentUser();
    }

    public void checkSize()
    {
        int newExists = getCurrentMailbox().getExists();
        if ( newExists != exists ) {
            _session.getOut().println( UNTAGGED + SP + newExists + " EXISTS" );
            exists = newExists;
        }
        int newRecent = getCurrentMailbox().getRecent();
        if ( newRecent != recent ) {
            _session.getOut().println( UNTAGGED + SP + newRecent + " RECENT" );
            recent = newRecent;
        }
        return;
    }

    public void checkExpunge()
    {
        List newList = getCurrentMailbox().listUIDs( _session.getCurrentUser() );
        for ( int k = 0; k < newList.size(); k++ ) {
            getLogger().debug( "New List msn " + (k + 1) + " is uid " + newList.get( k ) );
        }
        for ( int i = sequence.size() - 1; i > -1; i-- ) {
            Integer j = (Integer) sequence.get( i );
            getLogger().debug( "Looking for old msn " + (i + 1) + " was uid " + j );
            if ( !newList.contains( (Integer) sequence.get( i ) ) ) {
                _session.getOut().println( UNTAGGED + SP + (i + 1) + " EXPUNGE" );
            }
        }
        sequence = newList;
        //newList = null;
        return;
    }

    public ImapSessionState getState()
    {
        return state;
    }

    public void setState( ImapSessionState state )
    {
        this.state = state;
        exists = -1;
        recent = -1;
    }

    public BufferedReader getIn()
    {
        return in;
    }

    public void setIn( BufferedReader in )
    {
        this.in = in;
    }

    public PrintWriter getOut()
    {
        return out;
    }

    public void setOut( PrintWriter out )
    {
        this.out = out;
    }

    public String getRemoteHost()
    {
        return remoteHost;
    }

    public String getRemoteIP()
    {
        return remoteIP;
    }

    public Logger getDebugLogger()
    {
        return getLogger();
    }

    public Logger getSecurityLogger()
    {
        return securityLogger;
    }

    public UsersRepository getUsers()
    {
        return users;
    }

    public IMAPSystem getImapSystem()
    {
        return imapSystem;
    }

    public Host getImapHost()
    {
        return imapHost;
    }

    public String getCurrentNamespace()
    {
        return currentNamespace;
    }

    public void setCurrentNamespace( String currentNamespace )
    {
        this.currentNamespace = currentNamespace;
    }

    public String getCurrentSeperator()
    {
        return currentSeperator;
    }

    public void setCurrentSeperator( String currentSeperator )
    {
        this.currentSeperator = currentSeperator;
    }

    public String getCurrentFolder()
    {
        return currentFolder;
    }

    public void setCurrentFolder( String currentFolder )
    {
        this.currentFolder = currentFolder;
    }

    public ACLMailbox getCurrentMailbox()
    {
        return currentMailbox;
    }

    public void setCurrentMailbox( ACLMailbox currentMailbox )
    {
        this.currentMailbox = currentMailbox;
    }

    public boolean isCurrentIsReadOnly()
    {
        return currentIsReadOnly;
    }

    public void setCurrentIsReadOnly( boolean currentIsReadOnly )
    {
        this.currentIsReadOnly = currentIsReadOnly;
    }

    public boolean isConnectionClosed()
    {
        return connectionClosed;
    }

    public void setConnectionClosed( boolean connectionClosed )
    {
        this.connectionClosed = connectionClosed;
    }

    public String getCurrentUser()
    {
        return user;
    }

    public void setCurrentUser( String user )
    {
        this.user = user;
    }

    public void setSequence( List sequence )
    {
        this.sequence = sequence;
    }

    public List decodeSet( String rawSet, int exists ) throws IllegalArgumentException
    {
        return super.decodeSet( rawSet, exists );
    }
    
    public void setCanParseCommand(boolean canParseCommand) {
        this.canParseCommand = canParseCommand;
    }
    public boolean getCanParseCommand() {
        return this.canParseCommand;
    }
}
