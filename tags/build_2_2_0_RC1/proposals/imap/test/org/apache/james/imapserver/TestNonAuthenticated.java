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
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.net.Socket;
import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;

import org.apache.james.test.SimpleFileProtocolTest;
import org.apache.james.remotemanager.UserManagementTest;

public class TestNonAuthenticated
        extends SimpleFileProtocolTest
{
    public TestNonAuthenticated( String name )
    {
        super( name );
        _port = 143;
    }

    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "Welcome.test", _preElements );
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestNonAuthenticated( "Capability" ) );
        suite.addTest( new TestNonAuthenticated( "Authenticate" ) );
        suite.addTest( new TestNonAuthenticated( "Login" ) );
        suite.addTest( new TestNonAuthenticated( "Logout" ) );

        return suite;
    }

}
