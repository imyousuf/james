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

package org.apache.james.remotemanager;

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

public class TestRemoteManager
        extends SimpleFileProtocolTest
{

    public TestRemoteManager( String testFileName ) throws Exception
    {
        super( testFileName );
        _port = 4555;
        addTestFile( "RemoteManagerLogin.test", _preElements );
        addTestFile( "RemoteManagerLogout.test", _postElements );
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestRemoteManager( "AddUsers" ) );
        suite.addTest( new TestRemoteManager( "DeleteUsers" ) );
        return suite;
    }
}
