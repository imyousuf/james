/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

import org.apache.james.test.SimpleFileProtocolTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Runs tests for commands valid in the AUTHENTICATED state. A login session precedes
 * the execution of the test elements.
 */
public class TestCommandsInAuthenticatedState
        extends SimpleFileProtocolTest implements ImapTest
{
    public TestCommandsInAuthenticatedState( String name )
    {
        super( name );
    }

    /**
     * Sets up {@link #preElements} with a welcome message and login request/response.
     * @throws Exception
     */
    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "Welcome.test", preElements );
        addLogin( USER, PASSWORD );
    }

    protected void addLogin( String username, String password )
    {
        preElements.CL( "a001 LOGIN " + username + " " + password );
        preElements.SL( "a001 OK LOGIN completed", "TestCommandsInAuthenticatedState.java:33" );
    }

    /**
     * Provides all tests which should be run in the authenicated state. Each test name
     * corresponds to a protocol session file.
     */
    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        // Not valid in this state
        suite.addTest( new TestCommandsInAuthenticatedState( "ValidSelected" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "ValidNonAuthenticated" ) );

        // Valid in all states
        suite.addTest( new TestCommandsInAuthenticatedState( "Capability" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Noop" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Logout" ) );

        // Valid in authenticated state
        suite.addTest( new TestCommandsInAuthenticatedState( "ExamineInbox" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "SelectInbox" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Create" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "ExamineEmpty" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "SelectEmpty" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "ListNamespace" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "ListMailboxes" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Status" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Subscribe" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Delete" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Append" ) );

        return suite;
    }

}
