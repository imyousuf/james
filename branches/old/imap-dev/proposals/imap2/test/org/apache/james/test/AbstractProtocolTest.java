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

package org.apache.james.test;

import junit.framework.TestCase;
import org.apache.james.imapserver.ImapHandler;
import org.apache.james.imapserver.ImapHost;
import org.apache.james.imapserver.ImapRequestHandler;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionImpl;
import org.apache.james.imapserver.ImapTest;
import org.apache.james.imapserver.JamesImapHost;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.userrepository.AbstractUsersRepository;
import org.apache.james.userrepository.DefaultUser;
import org.apache.mailet.User;
import org.apache.mailet.UsersRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Abstract Protocol Test is the root of all of the James Imap Server test
 * cases.  It provides basic functionality for running a protocol session
 * as a JUnit test, and failing if exceptions are thrown.
 * To create a test which reads the entire protocol session from a single
 * protocol definition file, use the {@link SimpleFileProtocolTest}.
 *
 * @author Darrell DeBoer
 * @author Andrew C. Oliver
 */
public abstract class AbstractProtocolTest
        extends TestCase implements ImapTest
{
    /** The Protocol session which is run before the testElements */
    protected ProtocolSession preElements = new ProtocolSession();
    /** The Protocol session which contains the tests elements */
    protected ProtocolSession testElements = new ProtocolSession();
    /** The Protocol session which is run after the testElements. */
    protected ProtocolSession postElements = new ProtocolSession();

    /** The host name to connect to for socket-based testing. */
    protected String host = HOST;
    /** The host port to connect to for socket-based testing. */
    protected int port = PORT;
    /** The timeout to set on the socket for socket-based testing. */
    protected int timeout = TIMEOUT;

    /** A UsersRepository which all tests share. */
    private static UsersRepository users;
    /** An ImapHost instance which all tests share. */
    private static ImapHost imapHost;

    public AbstractProtocolTest( String s )
    {
        super( s );
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        setUpEnvironment();
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
            throws Exception {
        Socket[] socket = new Socket[testElements.getSessionCount()];
        PrintWriter[] out = new PrintWriter[socket.length];
        BufferedReader[] in = new BufferedReader[socket.length];

        for (int i = 0; i < socket.length; i++) {
            socket[i] = new Socket(host, port);
            socket[i].setSoTimeout(timeout);
            out[i] = new PrintWriter(socket[i].getOutputStream());
            in[i] = new BufferedReader(new InputStreamReader(socket[i].getInputStream()));
        }

        Exception failure = null;
        try {
            preElements.runLiveSession(out, in);
            testElements.runLiveSession(out, in);
        } catch (ProtocolSession.InvalidServerResponseException e) {
            failure = e;
        } finally {
            // Try our best to do cleanup.
            try {
                postElements.runLiveSession(out, in);
            } catch (ProtocolSession.InvalidServerResponseException e) {
                // Don't overwrite real error with error on cleanup.
                if (failure == null) {
                    failure = e;
                }
            }
        }
        
        if (failure != null) {
            fail(failure.getMessage());
        }

        for (int i = 0; i < socket.length; i++) {
            out[i].close();
            in[i].close();
            socket[i].close();
        }
    }

    /**
     * Runs the pre,test and post protocol sessions against a local copy of the ImapServer.
     * This does not require that James be running, and is useful for rapid development and
     * debugging.
     *
     * Instead of sending requests to a socket connected to a running instance of James,
     * this method uses the {@link MockImapServer} to simplify testing. One mock instance
     * is required per protocol session/connection. These share the same underlying 
     * Mailboxes, because of the way {@link MockImapServer#getImapSession()} works.
     */
    private void runLocalProtocolSessions() throws Exception
    {
        MockImapServer[] socket = new MockImapServer[testElements.getSessionCount()];
        PrintWriter[] out = new PrintWriter[socket.length];
        BufferedReader[] in = new BufferedReader[socket.length];

        for (int i = 0; i < socket.length; i++) {
            socket[i] = new MockImapServer();
            out[i] = socket[i].getWriter();
            in[i] = socket[i].getReader();
            socket[i].start();
        }

        Exception failure = null;
        try {
             preElements.runLiveSession( out, in );
             testElements.runLiveSession( out, in );
        } catch (ProtocolSession.InvalidServerResponseException e) {
             failure = e;
             // Try our best to do cleanup.
               for (int i = 0; i < in.length; i++) {
                   BufferedReader reader = in[i];
                   while (reader.ready()) {
                       reader.read();
                   }
               }
         } finally {
             try {
                 postElements.runLiveSession(out, in);
             } catch (ProtocolSession.InvalidServerResponseException e) {
                 // Don't overwrite real error with error on cleanup.
                 if (failure == null) {
                     failure = e;
                 }
             }
         }
        
         if (failure != null) {
             fail(failure.getMessage());
         }

 
        for (int i = 0; i < socket.length; i++) {
            out[i].close();
            in[i].close();
            socket[i].stopServer();
        }

    }

    /**
     * Initialises the UsersRepository and ImapHost on first call.
     * TODO enable logging, set up components properly.
     */
    private void setUpEnvironment() throws MailboxException
    {
        if ( users == null || imapHost == null ) {
            users = new InMemoryUsersRepository();
            DefaultUser user = new DefaultUser( USER, "SHA" );
            user.setPassword( PASSWORD );
            users.addUser( user );

            imapHost = new JamesImapHost();
            imapHost.createPrivateMailAccount( user );
        }
    }

    /**
     * A simple, dummy implementation of UsersRepository to use for testing,
     * which stored all users in memory.
     */
    private class InMemoryUsersRepository extends AbstractUsersRepository
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

    /**
     * A simple test utility which allows testing of Imap commands without
     * deployment of the full server. Piped input and output streams are used
     * to provide the equivilant of a socket interface.
     */ 
    private class MockImapServer extends Thread {
        private ImapRequestHandler handler = new ImapRequestHandler();

        private PipedInputStream requestInputStream;
        private PipedOutputStream requestOutputStream;

        private PipedInputStream responseInputStream;
        private PipedOutputStream responseOutputStream;

        private ImapSession session;
        private ProtocolException exception;

        private boolean running;

        /**
         * Creates a MockImapServer, with a handler for the session provided.
         */ 
        public MockImapServer() throws IOException {
            this.session = getImapSession();
            requestOutputStream = new PipedOutputStream();
            requestInputStream = new PipedInputStream(requestOutputStream);

            responseInputStream = new PipedInputStream();
            responseOutputStream = new PipedOutputStream(responseInputStream);

        }

        /**
         * Core loop where Imap commands are handled
         */ 
        public void run() {
            running = true;
            try {
                responseOutputStream.write( "* OK IMAP4rev1 Server XXX ready".getBytes() );
                responseOutputStream.write( '\r' );
                responseOutputStream.write( '\n' );
            } catch (IOException e) {
                throw new RuntimeException("Couldn't write welcome message", e);
            }

            while (running) {
                try {
                    handler.handleRequest(requestInputStream, responseOutputStream, session);
                } catch (ProtocolException e) {
                    exception = e;
                    break;
                }
            }
        }

        /** 
         * @return A writer which is used to send commands to the mock server.
         */ 
        public PrintWriter getWriter() {
            return new PrintWriter(requestOutputStream);
        }

        /**
         * @return A reader which is used to read responses from the mock server.
         */ 
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(responseInputStream));
        }

        /** stop the running server thread.*/
        public void stopServer() {
            running = false;
        }

        /**
         * Provides an ImapSession to use for this test. An ImapSession is accosiated
         * with a single client connection.
         */
        private ImapSession getImapSession()
        {
            ImapSession session = new ImapSessionImpl( imapHost, users, new MockImapHandler(), null, null );
            return session;
        }

        private final class MockImapHandler extends ImapHandler {
            public void forceConnectionClose(String message) {
                ImapResponse response = new ImapResponse(responseOutputStream);
                response.byeResponse(message);        
            }
        }
    }
}
