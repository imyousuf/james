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

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.Socket;
import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;

import org.apache.james.test.SimpleFileProtocolTest;
import org.apache.james.remotemanager.UserManagementTest;

public class TestAuthenticated
        extends SimpleFileProtocolTest implements IMAPTest
{
    public TestAuthenticated( String name )
    {
        super( name );
        _port = 143;
    }

    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "Welcome.test", _preElements );
        addLogin( USER, PASSWORD );
    }

    protected void addLogin( String username, String password )
    {
        _testElements.add( new ClientRequest( "a001 LOGIN " + username + " " + password ) );
        _testElements.add( new ServerResponse( "a001 OK LOGIN completed" ));
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestAuthenticated( "Capability" ) );
        suite.addTest( new TestAuthenticated( "AuthenticateAuthenticated" ) );
        suite.addTest( new TestAuthenticated( "LoginAuthenticated" ) );
        suite.addTest( new TestAuthenticated( "Logout" ) );
        suite.addTest( new TestAuthenticated( "ExamineInbox" ) );
        suite.addTest( new TestAuthenticated( "SelectInbox" ) );
        suite.addTest( new TestAuthenticated( "Create" ) );
        suite.addTest( new TestAuthenticated( "ExamineEmpty" ) );
        suite.addTest( new TestAuthenticated( "SelectEmpty" ) );
        suite.addTest( new TestAuthenticated( "SelectInbox" ) );
        suite.addTest( new TestAuthenticated( "List" ) );
        suite.addTest( new TestAuthenticated( "List1" ) );
        suite.addTest( new TestAuthenticated( "List2" ) );

        suite.addTest( new TestAuthenticated( "Subscribe" ) );
        suite.addTest( new TestAuthenticated( "Subscribe2" ) );

        suite.addTest( new TestAuthenticated( "Delete" ) );

        return suite;
    }

}
