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
