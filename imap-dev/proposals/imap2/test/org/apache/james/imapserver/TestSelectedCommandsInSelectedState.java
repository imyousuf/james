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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Runs tests for commands valid only in the SELECTED state. A login session
 * and setup of a "seleted" mailbox precedes the execution of the test elements.
 */
public class TestSelectedCommandsInSelectedState
        extends TestCommandsInAuthenticatedState
{
    public TestSelectedCommandsInSelectedState( String name )
    {
        super( name );
    }

    /**
     * Superclass sets up welcome message and login session in {@link #preElements}.
     * A "SELECT INBOX" session is then added to these elements.
     * @throws Exception
     */
    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "SelectedStateSetup.test", preElements );
        addTestFile( "SelectedStateCleanup.test", postElements );
    }

    /**
     * Provides all tests which should be run in the selected state. Each test name
     * corresponds to a protocol session file.
     */
    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();

        // Valid in selected state
        suite.addTest( new TestSelectedCommandsInSelectedState( "Check" ) );
        suite.addTest( new TestSelectedCommandsInSelectedState( "Expunge" ) );
        suite.addTest( new TestSelectedCommandsInSelectedState( "Search" ) );
        suite.addTest( new TestSelectedCommandsInSelectedState( "FetchSingleMessage" ) );
        suite.addTest( new TestSelectedCommandsInSelectedState( "FetchMultipleMessages" ) );
        suite.addTest( new TestSelectedCommandsInSelectedState( "FetchPeek" ) );
        suite.addTest( new TestSelectedCommandsInSelectedState( "Store" ) );
        suite.addTest( new TestSelectedCommandsInSelectedState( "Copy" ) );
        suite.addTest( new TestSelectedCommandsInSelectedState( "Uid" ) );

        return suite;
    }
}
