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

package org.apache.james.test.functional.imap;

import java.io.BufferedReader;
import java.io.PrintWriter;

import junit.framework.TestCase;


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
        extends TestCase implements ImapTestConstants
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

    private final HostSystem hostSystem;
    
    public AbstractProtocolTest( String name, HostSystem hostSystem )
    {
        super(name);
        this.hostSystem = hostSystem;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        setUpEnvironment();
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
    protected void runSessions() throws Exception
    {
        HostSystem.Session[] socket = new HostSystem.Session[testElements.getSessionCount()];
        PrintWriter[] out = new PrintWriter[socket.length];
        BufferedReader[] in = new BufferedReader[socket.length];

        for (int i = 0; i < socket.length; i++) {
            socket[i] = hostSystem.newSession();
            out[i] = new PrintWriter(socket[i].getWriter());
            in[i] = new BufferedReader(socket[i].getReader());
            socket[i].start();
        }
        try
        {
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

        }
        finally
        {
            for (int i = 0; i < socket.length; i++) {
                out[i].close();
                in[i].close();
                socket[i].stop();
            }
        }
    }

    /**
     * Initialises the UsersRepository and ImapHost on first call.
     */
    private void setUpEnvironment() throws Exception
    {
        hostSystem.reset();
        hostSystem.addUser( USER, PASSWORD );
    }
}
