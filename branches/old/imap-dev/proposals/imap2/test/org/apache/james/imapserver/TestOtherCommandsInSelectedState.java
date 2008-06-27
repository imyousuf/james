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
 * Tests commands which are valid in AUTHENTICATED and NONAUTHENTICATED by running
 * them in the SELECTED state. Many commands function identically, while others
 * are invalid in this state.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.5 $
 */
public class TestOtherCommandsInSelectedState
        extends TestCommandsInAuthenticatedState
{
    public TestOtherCommandsInSelectedState( String name )
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
        addTestFile( "SelectInbox.test", preElements );
    }

    protected void addCloseInbox()
    {
        postElements.CL( "a CLOSE");
        postElements.SL( ".*", "TestOtherCommandsInSelectedState.java:96");
    }
    
    /**
     * Provides all tests which should be run in the selected state. Each test name
     * corresponds to a protocol session file.
     */
    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        // Not valid in this state
        suite.addTest( new TestOtherCommandsInSelectedState( "ValidNonAuthenticated" ) );

        // Valid in all states
        suite.addTest( new TestOtherCommandsInSelectedState( "Capability" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "Noop" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "Logout" ) );

        // Valid in authenticated state
        suite.addTest( new TestOtherCommandsInSelectedState( "Create" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "ExamineEmpty" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "SelectEmpty" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "ListNamespace" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "ListMailboxes" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "Status" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "StringArgs" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "Subscribe" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "Append" ) );
        suite.addTest( new TestOtherCommandsInSelectedState( "Delete" ) );

        return suite;
    }
}
