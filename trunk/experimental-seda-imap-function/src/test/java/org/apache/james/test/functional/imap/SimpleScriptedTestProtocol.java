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

import java.io.InputStream;

/**
 * A Protocol test which reads the test protocol session from a file. The
 * file read is taken as "<test-name>.test", where <test-name> is the value
 * passed into the constructor.
 * Subclasses of this test can set up {@link #preElements} and {@link #postElements}
 * for extra elements not defined in the protocol session file.
 */
public class SimpleScriptedTestProtocol
        extends AbstractProtocolTest
{
    private FileProtocolSessionBuilder builder =
            new FileProtocolSessionBuilder();

    /**
     * Sets up a SimpleFileProtocolTest which reads the protocol session from
     * a file of name "<fileName>.test". This file should be available in the classloader
     * in the same location as this test class.
     * @param fileName The name of the file to read protocol elements from.
     */
    public SimpleScriptedTestProtocol( String fileName, HostSystem hostSystem  )
    {
        super( fileName, hostSystem );
    }

    /**
     * Reads test elements from the protocol session file and adds them to the
     * {@link #testElements} ProtocolSession. Then calls {@link #runSessions}.
     */
    protected void runTest() throws Throwable
    {
        String fileName = getName() + ".test";
        addTestFile( fileName, testElements );

        runSessions();
    }

    /**
     * Finds the protocol session file identified by the test name, and builds
     * protocol elements from it. All elements from the definition file are added
     * to the supplied ProtocolSession.
     * @param fileName The name of the file to read
     * @param session The ProtocolSession to add elements to.
     */ 
    protected void addTestFile( String fileName, ProtocolSession session) throws Exception
    {
        // Need to find local resource.
        InputStream is = this.getClass().getResourceAsStream( fileName );
        if ( is == null ) {
            throw new Exception( "Test Resource '" + fileName + "' not found." );
        }

        builder.addProtocolLinesFromStream( is, session, fileName );
    }
}
