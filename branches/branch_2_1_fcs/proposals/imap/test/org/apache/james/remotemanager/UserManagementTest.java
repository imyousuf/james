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

package org.apache.james.remotemanager;

import org.apache.james.test.AbstractProtocolTest;

public class UserManagementTest
        extends AbstractProtocolTest
{
    private String _userName;
    private String _password;

    public UserManagementTest( String action, String userName )
    {
        this( action, userName, "password" );
    }

    public UserManagementTest( String action, String userName, String password )
    {
        super( action );
        _port = 4555;
        _userName = userName;
        _password = password;
    }

    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "RemoteManagerLogin.test", _preElements );
        addTestFile( "RemoteManagerLogout.test", _postElements );
    }

    public void addUser() throws Exception
    {
          addUser( _userName, _password );
    }

    protected void addUser( String userName, String password )
            throws Exception
    {
        CL( "adduser " + userName + " " + password );
        SL( "User " + userName + " added" );
        executeTests();
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
        CL( "deluser " + userName );
        SL( "User " + userName + " deleted" );
        executeTests();
    }
}
