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
        testElements.SL( "User " + userName + " deleted" );
        runSessions();
    }
}
