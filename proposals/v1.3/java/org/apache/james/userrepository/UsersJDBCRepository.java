/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.userrepository;

import org.apache.james.services.*;
import org.apache.mailet.MailAddress;
import org.apache.avalon.framework.activity.*;
import org.apache.avalon.framework.component.*;
import org.apache.avalon.framework.configuration.*;
import org.apache.avalon.framework.logger.*;
import org.apache.avalon.excalibur.datasource.*;

import java.sql.*;
import java.util.*;

/**
 * An implementation of a UsersRepository which is backed by a database.<br>
 * This implementation has been tested successfully on:
 * <TABLE BORDER="1" WIDTH="100%">
 * <TR><TH>Database Product</TH><TH>versions</TH><TH>drivers</TH></TR>
 * <TR><TD>MySQL Server</TD><TD>3.23</TD><TD>MM MySQL 2.0.3</TD></TR>
 * <TR><TD>MySQL Server</TD><TD>3.23</TD><TD>JDBC-ODBC bridge</TD></TR>
 * <TR><TD>Oracle8i</TD><TD>8.1.6</TD><TD>Oracle Thin Driver</TD></TR>
 * <TR><TD>Microsoft SQL Server</TD><TD>7</TD><TD>Inet Sprinta</TD></TR>
 * </TABLE>
 * <br>
 * This implementation is known to fail on:
 * <TABLE BORDER="1" WIDTH="100%">
 * <TR><TH>Database Product</TH><TH>versions</TH><TH>drivers</TH></TR>
 * <TR><TD>Microsoft SQL Server</TD><TD>7</TD><TD>JDBC-ODBC bridge.</TD></TR>
 * </TABLE>
 * 
 * @author Darrell DeBoer <dd@bigdaz.com>
 */
public class UsersJDBCRepository extends AbstractUsersRepository
    implements UsersRepository, Loggable, Component, Configurable, Initializable
{
    /*
    TODO
        - allow configurable SQL statements.
        - use Connection pooling and management from Avalon framework.
    */
    private String m_dbUrl;
    private String m_dbUser;
    private String m_dbPassword;

    private String m_tableName = "JamesUsers";
    
    // Fetches all Users from the db.
    private String m_getUsersSql = "SELECT username, pwdHash, pwdAlgorithm, useForwarding, forwardDestination, useAlias, alias FROM " + m_tableName;
    
    // This statement guarantees that we'll get a case-insensitive match - otherwise it's database dependent.
    private String m_userByNameCaseInsensitiveSql = m_getUsersSql + " WHERE LOWER(username) = ?";

    // Insert, update and delete are not guaranteed to be case-insensitive
    // this is handled in code.
    private String m_insertUserSql = "INSERT INTO " + m_tableName + " (username, pwdHash, pwdAlgorithm, useForwarding, forwardDestination, useAlias, alias) VALUES (?,?,?,?,?,?,?)";
    private String m_updateUserSql = "UPDATE " + m_tableName + " SET pwdHash = ?, pwdAlgorithm = ?, useForwarding = ?, forwardDestination = ?, useAlias = ?, alias = ? WHERE username = ?";
    private String m_deleteUserSql = "DELETE FROM " + m_tableName + " WHERE username = ?";

    // Creates a single table with "username" the Primary Key.
    private String m_createUserTableSql = "CREATE TABLE " + m_tableName + " (username VARCHAR(50) NOT NULL, pwdHash VARCHAR(50), pwdAlgorithm VARCHAR(20), useForwarding SMALLINT, forwardDestination VARCHAR(250), useAlias SMALLINT, alias VARCHAR(250), PRIMARY KEY(username))";

    /**
     * Configures the UserRepository for JDBC access.
     * Required parameters in the config file are:
     * <UL>
     *     <LI>destination.driver.class - the class of the JDBC driver to load.</LI>
     *     <LI>destination.datasource.dburl - the database connection string.</LI>
     * </UL>
     * Optional parameters are:
     * <UL>
     *     <LI>destination.datasource.user - username to connect to the database.</LI>
     *     <LI>destination.datasource.password - password to connect to the database.</LI>
     * </UL>
     */
    public void configure(Configuration configuration) throws ConfigurationException 
    {
        // Load the driver.

        String driverName = configuration.getChild("destination").getChild("driver").getAttribute("class");
	getLogger().debug("Loading driver :" + driverName);
        try {
            Class.forName(driverName);
	    getLogger().info("Database driver " + driverName + " loaded");
        } 
        catch ( ClassNotFoundException cnfe ) {
            throw new ConfigurationException("Could not load specified driver - " + driverName);
        }

        // Get the database connection configuration.
        Configuration dbConfig = configuration.getChild("destination").getChild("datasource");
        m_dbUrl = dbConfig.getChild("dburl").getValue();
        m_dbUser = dbConfig.getChild("user").getValue(null);
        m_dbPassword = dbConfig.getChild("password").getValue(null);
    }

    /**
     * Initialises the JDBC repository.
     * 1) Tests the connection to the database.
     * 2) Initialises the database with the required tables, if necessary.
     * 
     * @exception Exception if a database access error occurs.
     */
    public void initialize() throws Exception {
        // Test the connection to the database, by getting the DatabaseMetaData.
        Connection conn = getConnection();
        try{
            DatabaseMetaData dbMetaData = conn.getMetaData();

            // Make sure that the Users table is there, if not create it.

            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // NB this should work, but some drivers (eg mm MySQL) 
            // don't return the right details, hence the hackery below.
            /*
            String tableName = m_tableName;
            if ( dbMetaData.storesLowerCaseIdentifiers() ) {
                tableName = tableName.toLowerCase();
            }
            else if ( dbMetaData.storesUpperCaseIdentifiers() ) {
                tableName = tableName.toUpperCase();
            }
            */

            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (! ( tableExists(dbMetaData, m_tableName) ||
                    tableExists(dbMetaData, m_tableName.toUpperCase()) ||
                    tableExists(dbMetaData, m_tableName.toLowerCase()) )) 
            {
                // Users table doesn't exist - create it.
                PreparedStatement createStatement = conn.prepareStatement(m_createUserTableSql);
                createStatement.execute();
                createStatement.close();
		getLogger().info("Created \'JamesUsers\' table.");
                System.out.println("UsersStore - UsersJDBCRepository : Created \'JamesUsers\' table.");
            }
        }
        finally {
            conn.close();
        }
    }

    private boolean tableExists(DatabaseMetaData dbMetaData, String tableName)
        throws SQLException
    {
        ResultSet rsTables = dbMetaData.getTables(null, null, tableName, null);
        boolean found = rsTables.next();
        rsTables.close();
        return found;
    }

    //
    // Superclass methods.
    //
    /**
     * Returns a list populated with all of the Users in the repository.
     * @return an <code>Iterator</code> of <code>DefaultJamesUser</code>s.
     */
    protected Iterator listAllUsers() 
    {
        List userList = new LinkedList(); // Build the users into this list.

        Connection conn = getConnection();
        try {
            // Get a ResultSet containing all users.
            PreparedStatement getUsersStatement = 
                getConnection().prepareStatement(m_getUsersSql);
            ResultSet rsUsers = getUsersStatement.executeQuery();

            // Loop through and build a JamesUser for every row.
            while ( rsUsers.next() ) {
                DefaultJamesUser user = readUserFromResultSet(rsUsers);
                userList.add(user);
            }

            rsUsers.close();
            getUsersStatement.close();
        }
        catch ( SQLException sqlExc) {
            sqlExc.printStackTrace();
            throw new RuntimeException("Error accessing database");
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException sqlExc) {
                sqlExc.printStackTrace();
                throw new RuntimeException("Error closing connection");
            }
        }

        return userList.iterator();
    }

    /**
     * Adds a user to the underlying Repository.
     * The user name must not clash with an existing user.
     */
    protected void doAddUser(DefaultJamesUser user)
    {
        // Get the user details to save.
        String username = user.getUserName();
        String pwdHash = user.getHashedPassword();
        String pwdAlgorithm = user.getHashAlgorithm();
        boolean useForwarding = user.getForwarding();
        MailAddress forwardAddress = user.getForwardingDestination();
        String forwardDestination = null;
        if ( forwardAddress != null ) {
            forwardDestination = forwardAddress.toString();
        }
        boolean useAlias = user.getAliasing();
        String alias = user.getAlias();

        Connection conn = getConnection();

        // Insert into the database.
        try {
            PreparedStatement addUserStatement = conn.prepareStatement(m_insertUserSql);
            
            addUserStatement.setString(1, username);
            addUserStatement.setString(2, pwdHash);
            addUserStatement.setString(3, pwdAlgorithm);
            addUserStatement.setBoolean(4, useForwarding);
            addUserStatement.setString(5, forwardDestination);
            addUserStatement.setBoolean(6, useAlias);
            addUserStatement.setString(7, alias);

            addUserStatement.execute();

            addUserStatement.close();
        }
        catch ( SQLException sqlExc ) {
            sqlExc.printStackTrace();
            throw new RuntimeException("Error accessing database");
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException sqlExc) {
                sqlExc.printStackTrace();
                throw new RuntimeException("Error closing connection");
            }
        }
    }

    /**
     * Removes a user from the underlying repository.
     * If the user doesn't exist, returns ok.
     */
    protected void doRemoveUser(DefaultJamesUser user)
    {
        String username = user.getUserName();

        Connection conn = getConnection();
        // Delete from the database.
        try {
            PreparedStatement removeUserStatement = conn.prepareStatement(m_deleteUserSql);
            removeUserStatement.setString(1, username);
            removeUserStatement.execute();
            removeUserStatement.close();
        }
        catch ( SQLException sqlExc ) {
            sqlExc.printStackTrace();
            throw new RuntimeException("Error accessing database");
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException sqlExc) {
                sqlExc.printStackTrace();
                throw new RuntimeException("Error closing connection");
            }
        }
    }

    /**
     * Gets a user by name, ignoring case if specified.
     * This overrides the basic implementation in AbstractUsersRepository
     * to increase performance.
     */
    protected User getUserByName(String name, boolean ignoreCase)
    {
        // Always get the user via case-insensitive SQL,
        // then check case if necessary.
        Connection conn = getConnection();
        try {
            // Get a ResultSet containing all users.
            String sql = m_userByNameCaseInsensitiveSql;
            PreparedStatement getUsersStatement = 
                getConnection().prepareStatement(sql);

            getUsersStatement.setString(1, name.toLowerCase());

            ResultSet rsUsers = getUsersStatement.executeQuery();

            // For case-insensitive matching, the first matching user will be returned.
            DefaultJamesUser user = null;
            while ( rsUsers.next() ) {
                DefaultJamesUser rowUser = readUserFromResultSet(rsUsers);
                String actualName = rowUser.getUserName();
                    
                // Check case before we assume it's the right one.
                if ( ignoreCase || actualName.equals(name) ) {
                    user = rowUser;
                    break;
                }
            }
            return user;
        }
        catch ( SQLException sqlExc ) {
            sqlExc.printStackTrace();
            throw new RuntimeException("Error accessing database");
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException sqlExc) {
                sqlExc.printStackTrace();
                throw new RuntimeException("Error closing connection");
            }
        }
    }

    /**
     * Updates a user record to match the supplied User.
     * This is a very basic, remove-then-insert implementation,
     * which should really be overridden, but will work.
     */
    protected void doUpdateUser(DefaultJamesUser user)
    {
        // Get the user details to save.
        String username = user.getUserName();
        String pwdHash = user.getHashedPassword();
        String pwdAlgorithm = user.getHashAlgorithm();
        boolean useForwarding = user.getForwarding();
        MailAddress forwardAddress = user.getForwardingDestination();
        String forwardDestination = null;
        if ( forwardAddress != null ) {
            forwardDestination = forwardAddress.toString();
        }
        boolean useAlias = user.getAliasing();
        String alias = user.getAlias();

        Connection conn = getConnection();

        // Insert into the database.
        try {
            PreparedStatement updateUserStatement = conn.prepareStatement(m_updateUserSql);
            
            updateUserStatement.setString(1, pwdHash);
            updateUserStatement.setString(2, pwdAlgorithm);
            updateUserStatement.setBoolean(3, useForwarding);
            updateUserStatement.setString(4, forwardDestination);
            updateUserStatement.setBoolean(5, useAlias);
            updateUserStatement.setString(6, alias);
            updateUserStatement.setString(7, username);

            updateUserStatement.execute();

            updateUserStatement.close();
        }
        catch ( SQLException sqlExc ) {
            sqlExc.printStackTrace();
            throw new RuntimeException("Error accessing database");
        }
        finally {
            try {
                conn.close();
            }
            catch (SQLException sqlExc) {
                sqlExc.printStackTrace();
                throw new RuntimeException("Error closing connection");
            }
        }
    }

    //
    // Private methods
    //
    /**
     * Opens a database connection.
     */
    private Connection getConnection()
    {
        try {
            if ( m_dbUser == null ) {
                return DriverManager.getConnection(m_dbUrl);
            }
            else {
                return DriverManager.getConnection(m_dbUrl, m_dbUser, m_dbPassword);
            }
        }
        catch ( SQLException sqlExc ) {
            sqlExc.printStackTrace();
            throw new RuntimeException("Error connecting to database");
        }
    }

    /**
     * Reads a user from the current row in a ResultSet.
     */
    private DefaultJamesUser readUserFromResultSet(ResultSet rsUsers)
        throws SQLException
    {
        // Get the column values
        String username = rsUsers.getString(1);
        String pwdHash = rsUsers.getString(2);
        String pwdAlgorithm = rsUsers.getString(3);
        boolean useForwarding = rsUsers.getBoolean(4);
        String forwardingDestination = rsUsers.getString(5);
        boolean useAlias = rsUsers.getBoolean(6);
        String alias = rsUsers.getString(7);

        MailAddress forwardAddress = null;
        if ( forwardingDestination != null ) {
            try {
                forwardAddress = new MailAddress(forwardingDestination);
            }
            catch (javax.mail.internet.ParseException pe) {
                throw new RuntimeException("Invalid mail address in database: " + forwardingDestination + ", for user " + username + ".");
            }
        }

        // Build a DefaultJamesUser with these values, and add to the list.
        DefaultJamesUser user = new DefaultJamesUser(username, pwdHash, pwdAlgorithm);
        user.setForwarding(useForwarding);
        user.setForwardingDestination(forwardAddress);
        user.setAliasing(useAlias);
        user.setAlias(alias);

        return user;
    }
}
