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

import org.apache.james.test.AbstractProtocolTest;
import org.apache.james.test.FileProtocolSessionBuilder;

public class UserManagementTest
        extends AbstractProtocolTest
{
    private String _userName;
    private String _password;
    private FileProtocolSessionBuilder builder = new FileProtocolSessionBuilder();

    public UserManagementTest( String action, String userName )
    {
        this( action, userName, "password" );
    }

    public UserManagementTest( String action, String userName, String password )
    {
        super( action );
        _userName = userName;
        _password = password;
    }

    public void setUp() throws Exception
    {
        super.setUp();
        builder.addTestFile( "RemoteManagerLogin.test", preElements );
        builder.addTestFile( "RemoteManagerLogout.test", postElements );
    }

    public void addUser() throws Exception
    {
          addUser( _userName, _password );
    }

    protected void addUser( String userName, String password )
            throws Exception
    {
        testElements.CL( "adduser " + userName + " " + password );
        testElements.SL( "User " + userName + " added", "Generated test." );
        runSessions();
    }

    /*protected void addExistingUser( String userName, String password )
        throws Exception{
        CL( "adduser " + userName + " " + password );
        SL( "user " + userName + " already exist" );
        executeTests();
    }*/

    public void deleteUser() throws Exception
    {
        deleteUser( _userName );
    }

    protected void deleteUser( String userName ) throws Exception
    {
        testElements.CL( "deluser " + userName );
        testElements.SL( "User " + userName + " deleted", "UserManagementTest.java:60" );
        runSessions();
    }
}
