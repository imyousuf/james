package org.apache.james.test;

import org.apache.james.imapserver.IMAPTest;
import org.apache.james.imapserver.ImapHandler;
import org.apache.james.imapserver.ImapHost;
import org.apache.james.imapserver.ImapRequestHandler;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionImpl;
import org.apache.james.imapserver.JamesImapHost;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.james.userrepository.AbstractUsersRepository;
import org.apache.james.userrepository.DefaultUser;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Abstract Protocol Test is the root of all of the James Imap Server test
 * cases.  It provides functionality to create text files for matching
 * client requests and server responses.  In order to use it however you
 * must create a sub class and set all the file names, etc up yourself.
 * All Comments are written by Andy Oliver who is still trying to figure out
 * some of it himself so don't take this as gospel
 *
 * @author Unattributed Original Authors
 * @author Andrew C. Oliver
 */
public abstract class AbstractProtocolTest
        extends TestCase implements IMAPTest
{
    protected ProtocolSession preElements = new ProtocolSession();
    protected ProtocolSession testElements;
    protected ProtocolSession postElements = new ProtocolSession();

    protected String host = HOST;
    protected int port = PORT;
    protected int timeout = TIMEOUT;

    private static UsersRepository users;
    private static ImapHost imapHost;

    public AbstractProtocolTest( String s )
    {
        super( s );
    }

    // comment in TestCase
    public void setUp() throws Exception
    {
        super.setUp();
        testElements = new ProtocolSession();

    }

    /**
     * Uses a system property to determine whether to run tests locally, or against
     * a remote server.
     */
    protected void runSessions() throws Exception
    {
        String runLocal = System.getProperty( "runTestsLocal", "true" );
        boolean local = Boolean.valueOf( runLocal ).booleanValue();
        if ( local ) {
            runLocalProtocolSessions();
        }
        else {
            runSocketProtocolSessions();
        }
    }

    /**
     * Runs the pre,test and post protocol sessions against a running instance of James,
     * by communicating via a socket connection. After a request is sent, the server response
     * is parsed to determine if the actual response matches that expected.
     */
    private void runSocketProtocolSessions()
            throws Exception
    {
        Socket socket = new Socket( host, port );
        socket.setSoTimeout( timeout );

        PrintWriter out = new PrintWriter( socket.getOutputStream(), true );
        BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
        try {
             preElements.runSession( in, out );
             testElements.runSession( in, out );
             postElements.runSession( in, out );
         }
         catch ( InvalidServerResponseException e ) {
             fail( e.getMessage() );
         }

        out.close();
        in.close();
        socket.close();
     }

    /**
     * Runs the pre,test and post protocol sessions against a local copy of the ImapServer.
     * This does not require that James be running, and is useful for rapid development and
     * debugging.
     *
     * Instead of sending requests to a socket, requests are written to a PipedWriter,
     * which then provides a Reader which can be given to the ImapRequestHandler.
     * Likewise, server responses are writter to a PipedWriter, and the associate reader
     * is parsed to ensure that the responses match those expected.
     */
    private void runLocalProtocolSessions() throws Exception
    {
        // Read the client requests into a piped reader.
        PipedReader clientPipeReader = new PipedReader();
        PipedWriter clientPipeWriter = new PipedWriter( clientPipeReader );
        PrintWriter clientOut = new PrintWriter( clientPipeWriter );
        preElements.writeClient( clientOut );
        testElements.writeClient( clientOut );
        postElements.writeClient( clientOut );

        clientPipeWriter.close();

        BufferedReader clientIn = new BufferedReader( clientPipeReader );

        PipedReader serverPipeReader = new PipedReader();
        PipedWriter serverPipeWriter = new PipedWriter( serverPipeReader );
        PrintWriter serverOut = new PrintWriter( serverPipeWriter );

        serverOut.println( "* OK IMAP4rev1 Server XXX ready" );

        ImapSession session = getImapSession();
        ImapRequestHandler requestHandler = new ImapRequestHandler();
        while( requestHandler.handleRequest( clientIn, serverOut, session ) ) {};

        BufferedReader serverIn = new BufferedReader( serverPipeReader );
        try {
            preElements.testResponse(  serverIn );
            testElements.testResponse(  serverIn );
            postElements.testResponse(  serverIn );
        }
        catch ( InvalidServerResponseException e ) {
            fail( e.getMessage() );
        }
    }

    // TODO setup the session properly.
    private ImapSession getImapSession() throws MailboxException
    {
        setUpEnvironment();
        ImapSession session = new ImapSessionImpl( imapHost, users, new ImapHandler(), null, null );
        return session;
    }

    private void setUpEnvironment() throws MailboxException
    {
        if ( users == null || imapHost == null ) {
            users = new DummyUsersRepository();
            DefaultUser user = new DefaultUser( USER, "SHA" );
            user.setPassword( PASSWORD );
            users.addUser( user );

            imapHost = new JamesImapHost();
            imapHost.createPrivateMailAccount( user );
        }
    }

    private class DummyUsersRepository extends AbstractUsersRepository
    {
        private Map users = new HashMap();

        protected Iterator listAllUsers()
        {
            return users.values().iterator();
        }

        protected void doAddUser( User user )
        {
            users.put( user.getUserName(), user );
        }

        protected void doRemoveUser( User user )
        {
            users.remove( user.getUserName());
        }

        protected void doUpdateUser( User user )
        {
            users.put( user.getUserName(), user );
        }
    }
}
