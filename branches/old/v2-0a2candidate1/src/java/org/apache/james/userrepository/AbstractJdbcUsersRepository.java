/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.userrepository;

import org.apache.james.services.*;
import org.apache.james.util.SqlResources;
import org.apache.mailet.MailAddress;
import org.apache.avalon.framework.*;
import org.apache.avalon.framework.activity.*;
import org.apache.avalon.framework.component.*;
import org.apache.avalon.framework.configuration.*;
import org.apache.avalon.framework.context.*;
import org.apache.avalon.framework.logger.*;
import org.apache.avalon.excalibur.datasource.*;
import org.apache.avalon.cornerstone.services.datasource.DataSourceSelector;
import org.apache.avalon.phoenix.BlockContext;

import java.sql.*;
import java.util.*;
import java.io.File;

/**
 * An abstract base class for creating UserRepository implementation
 * which use a database for persistence.
 * 
 * To implement a new UserRepository using by extending this class,
 * you need to implement the 3 abstract methods defined below,
 * and define the required SQL statements in an SQLResources
 * file.
 * 
 * The SQL statements used by this implementation are:
 * <TABLE>
 * <TH><TD><B>Required</B></TD></TH>
 * <TR><TD>select</TD><TD>Select all users.</TD></TR>
 * <TR><TD>insert</TD><TD>Insert a user.</TD></TR>
 * <TR><TD>update</TD><TD>Update a user.</TD></TR>
 * <TR><TD>delete</TD><TD>Delete a user by name.</TD></TR>
 * <TR><TD>createTable</TD><TD>Create the users table.</TD></TR>
 * <TH><TD><B>Optional</B></TD></TH>
 * <TR><TD>selectByLowercaseName</TD><TD>Select a user by name (case-insensitive lowercase).</TD></TR>
 * </TABLE>
 * 
 * @author Darrell DeBoer <dd@bigdaz.com>
 */
public abstract class AbstractJdbcUsersRepository extends AbstractUsersRepository
    implements UsersRepository, Loggable, Component, Contextualizable, Composable, Configurable, Initializable
{
    protected Context context;
    protected Map m_sqlParameters;
    private String m_sqlFileName;
    private String m_datasourceName;
    private DataSourceSelector m_datasources;
    private DataSourceComponent m_datasource;

    // Fetches all Users from the db.
    private String m_getUsersSql;
    
    // This fetch a user by name, ensuring case-insensitive matching.
    private String m_userByNameCaseInsensitiveSql;

    // Insert, update and delete sql statements are not guaranteed 
    //  to be case-insensitive; this is handled in code.
    private String m_insertUserSql;
    private String m_updateUserSql;
    private String m_deleteUserSql;

    // Creates a single table with "username" the Primary Key.
    private String m_createUserTableSql;


    public void contextualize(final Context context)
            throws ContextException {
        this.context = context;
    }

    /**
     * Compose the repository with the DataSourceSelector component.
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        getLogger().debug(this.getClass().getName() + ".compose()");

        m_datasources = 
            (DataSourceSelector)componentManager.lookup( DataSourceSelector.ROLE );
    }

    /**
     * Configures the UserRepository for JDBC access.
     * 
     * Requires a configuration element in the .conf.xml file of the form:
     * 
     *  <repository name="LocalUsers"
     *              class="org.apache.james.userrepository.JamesUsersJdbcRepository">
     *      <!-- Name of the datasource to use -->
     *      <data-source>MailDb</data-source>
     *      <!-- File to load the SQL definitions from -->
     *      <sqlFile>dist/conf/sqlResources.xml</sqlFile>
     *      <!-- replacement parameters for the sql file -->
     *      <sqlParameters table="JamesUsers"/>
     *  </repository>
     */
    public void configure(Configuration configuration) throws ConfigurationException 
    {
        getLogger().debug(this.getClass().getName() +  ".configure()");

        // Parse the DestinationURL for the name of the datasource, 
        // the table to use, and the (optional) repository Key.
        String destUrl = configuration.getAttribute("destinationURL");
        // normalise the destination, to simplify processing.
        if ( ! destUrl.endsWith("/") ) {
            destUrl += "/";
        }
        // Split on "/", starting after "db://"
        List urlParams = new LinkedList();
        int start = 5;
        int end = destUrl.indexOf('/', start);
        while ( end > -1 ) {
            urlParams.add(destUrl.substring(start, end));
            start = end + 1;
            end = destUrl.indexOf('/', start);
        }

        // Build SqlParameters and get datasource name from URL parameters
        m_sqlParameters = new HashMap();
        switch ( urlParams.size() ) {
        case 3:
            m_sqlParameters.put("key", urlParams.get(2));
        case 2:
            m_sqlParameters.put("table", urlParams.get(1));
        case 1:
            m_datasourceName = (String)urlParams.get(0);
            break;
        default:
            throw new ConfigurationException
                ("Malformed destinationURL - " +
                 "Must be of the format \"db://<data-source>[/<table>[/<key>]]\".");
        }

        getLogger().debug("Parsed URL: table = '" + m_sqlParameters.get("table") + 
                          "', key = '" + m_sqlParameters.get("key") + "'");
        
        // Get the SQL file location
        m_sqlFileName = configuration.getChild("sqlFile", true).getValue();
        if (!m_sqlFileName.startsWith("file://")) {
            throw new ConfigurationException
                ("Malformed sqlFile - Must be of the format \"file://<filename>\".");
        }

        // Get other sql parameters from the configuration object,
        // if any.
        Configuration sqlParamsConfig = configuration.getChild("sqlParameters");
        String[] paramNames = sqlParamsConfig.getAttributeNames();
        for (int i = 0; i < paramNames.length; i++ ) {
            String paramName = paramNames[i];
            String paramValue = sqlParamsConfig.getAttribute(paramName);
            m_sqlParameters.put(paramName, paramValue);
        }
    }

    /**
     * Initialises the JDBC repository.
     * 1) Tests the connection to the database.
     * 2) Loads SQL strings from the SQL definition file,
     *     choosing the appropriate SQL for this connection, 
     *     and performing paramter substitution,
     * 3) Initialises the database with the required tables, if necessary.
     * 
     */
    public void initialize() throws Exception 
    {
        getLogger().debug( this.getClass().getName() + ".initialize()");

        // Get the data-source required.
        m_datasource = (DataSourceComponent)m_datasources.select(m_datasourceName);

        // Test the connection to the database, by getting the DatabaseMetaData.
        Connection conn = openConnection();
        try{
            DatabaseMetaData dbMetaData = conn.getMetaData();

            // Initialise the sql strings.
            String fileName = m_sqlFileName.substring("file://".length());
            fileName = ((BlockContext)context).getBaseDirectory() +
                        File.separator + fileName;
            File sqlFile = (new File(fileName)).getCanonicalFile();
            
            getLogger().debug("Reading SQL resources from file: " + 
                              sqlFile.getAbsolutePath() + ", section " +
                              this.getClass().getName() + ".");

            SqlResources sqlStatements = new SqlResources();
            sqlStatements.init(sqlFile, this.getClass().getName(), 
                               conn, m_sqlParameters);

            // Create the SQL Strings to use for this table.
            // Fetches all Users from the db.
            m_getUsersSql = sqlStatements.getSqlString("select", true);

            // Get a user by lowercase name. (optional)
            // If not provided, the entire list is iterated to find a user.
            m_userByNameCaseInsensitiveSql = 
                sqlStatements.getSqlString("selectByLowercaseName");

            // Insert, update and delete are not guaranteed to be case-insensitive
            // Will always be called with correct case in username..
            m_insertUserSql = sqlStatements.getSqlString("insert", true);
            m_updateUserSql = sqlStatements.getSqlString("update", true);
            m_deleteUserSql = sqlStatements.getSqlString("delete", true);

            // Creates a single table with "username" the Primary Key.
            m_createUserTableSql = sqlStatements.getSqlString("createTable", true);

            // Check if the required table exists. If not, create it.
            // The table name is defined in the SqlResources.
            String tableName = sqlStatements.getSqlString("tableName", true);
            
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
            if (! ( tableExists(dbMetaData, tableName) ||
                    tableExists(dbMetaData, tableName.toUpperCase()) ||
                    tableExists(dbMetaData, tableName.toLowerCase()) )) 
            {
                // Users table doesn't exist - create it.
                PreparedStatement createStatement = 
                    conn.prepareStatement(m_createUserTableSql);
                createStatement.execute();
                createStatement.close();

                getLogger().info(this.getClass().getName() + ": Created table \'" + 
                                 tableName + "\'.");
            }
            else {
                getLogger().debug("Using table: " + tableName);
            }
        
        }
        finally {
            closeConnection( conn );
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
    // Superclass methods - overridden in AbstractUsersRepository
    //
    /**
     * Returns a list populated with all of the Users in the repository.
     * @return an <code>Iterator</code> of <code>JamesUser</code>s.
     */
    protected Iterator listAllUsers() {
        List userList = new LinkedList(); // Build the users into this list.

        Connection conn = openConnection();
        try {
            // Get a ResultSet containing all users.
            PreparedStatement getUsersStatement = 
                conn.prepareStatement(m_getUsersSql);
            ResultSet rsUsers = getUsersStatement.executeQuery();

            // Loop through and build a User for every row.
            while ( rsUsers.next() ) {
                User user = readUserFromResultSet(rsUsers);
                userList.add(user);
            }

            rsUsers.close();
            getUsersStatement.close();
        }
        catch ( SQLException sqlExc) {
            sqlExc.printStackTrace();
            throw new CascadingRuntimeException("Error accessing database", sqlExc);
        }
        finally {
            closeConnection(conn);
        }

        return userList.iterator();
    }

    /**
     * Adds a user to the underlying Repository.
     * The user name must not clash with an existing user.
     */
    protected void doAddUser(User user) {
        Connection conn = openConnection();
        // Insert into the database.
        try {
            // Get a PreparedStatement for the insert.
            PreparedStatement addUserStatement = 
                conn.prepareStatement(m_insertUserSql);

            setUserForInsertStatement(user, addUserStatement);

            addUserStatement.execute();
            addUserStatement.close();
        }
        catch ( SQLException sqlExc) {
            sqlExc.printStackTrace();
            throw new CascadingRuntimeException("Error accessing database", sqlExc);
        }
        finally {
            closeConnection(conn);
        }
    }

    /**
     * Removes a user from the underlying repository.
     * If the user doesn't exist, returns ok.
     */
    protected void doRemoveUser(User user) {
        String username = user.getUserName();

        Connection conn = openConnection();
        // Delete from the database.
        try {
            PreparedStatement removeUserStatement = conn.prepareStatement(m_deleteUserSql);
            removeUserStatement.setString(1, username);
            removeUserStatement.execute();
            removeUserStatement.close();
        }
        catch ( SQLException sqlExc ) {
            sqlExc.printStackTrace();
            throw new CascadingRuntimeException("Error accessing database", sqlExc);
        }
        finally {
            closeConnection(conn);
        }
    }

    /**
     * Updates a user record to match the supplied User.
     */
    protected void doUpdateUser(User user)
    {
        Connection conn = openConnection();

        // Update the database.
        try {
            PreparedStatement updateUserStatement = conn.prepareStatement(m_updateUserSql);

            setUserForUpdateStatement(user, updateUserStatement);

            updateUserStatement.execute();

            updateUserStatement.close();
        }
        catch ( SQLException sqlExc ) {
            sqlExc.printStackTrace();
            throw new CascadingRuntimeException("Error accessing database", sqlExc);
        }
        finally {
            closeConnection(conn);
        }
    }

    /**
     * Gets a user by name, ignoring case if specified.
     * If the specified SQL statement has been defined, this method
     * overrides the basic implementation in AbstractUsersRepository
     * to increase performance.
     */
    protected User getUserByName(String name, boolean ignoreCase)
    {
        // See if this statement has been set, if not, use
        // simple superclass method.
        if ( m_userByNameCaseInsensitiveSql == null ) {
            return super.getUserByName(name, ignoreCase);
        }

        // Always get the user via case-insensitive SQL,
        // then check case if necessary.
        Connection conn = openConnection();
        try {
            // Get a ResultSet containing all users.
            String sql = m_userByNameCaseInsensitiveSql;
            PreparedStatement getUsersStatement = 
                conn.prepareStatement(sql);

            getUsersStatement.setString(1, name.toLowerCase());

            ResultSet rsUsers = getUsersStatement.executeQuery();

            // For case-insensitive matching, the first matching user will be returned.
            User user = null;
            while ( rsUsers.next() ) {
                User rowUser = readUserFromResultSet(rsUsers);
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
            throw new CascadingRuntimeException("Error accessing database", sqlExc);
        }
        finally {
            closeConnection(conn);
        }
    }


    /**
     * Reads properties for a User from an open ResultSet.
     * Subclass implementations of this method must have knowledge of the fields
     * presented by the "select" and "selectByLowercaseName" SQL statements.
     * These implemenations may generate a subclass-specific User instance.
     * 
     * @param rsUsers A ResultSet with a User record in the current row.
     * @return A User instance
     * @exception SQLException
     *                   if an exception occurs reading from the ResultSet
     */
    protected abstract User readUserFromResultSet(ResultSet rsUsers)
        throws SQLException;

    /**
     * Set parameters of a PreparedStatement object with 
     * property values from a User instance.
     * Implementations of this method have knowledge of the parameter
     * ordering of the "insert" SQL statement definition.
     * 
     * @param user       a User instance, which should be an implementation class which
     *                   is handled by this Repostory implementation.
     * @param userInsert a PreparedStatement initialised with SQL taken from the "insert" SQL definition.
     * @exception SQLException
     *                   if an exception occurs while setting parameter values.
     */
    protected abstract void setUserForInsertStatement(User user, 
                                                      PreparedStatement userInsert)
        throws SQLException;

    /**
     * Set parameters of a PreparedStatement object with
     * property values from a User instance.
     * Implementations of this method have knowledge of the parameter
     * ordering of the "update" SQL statement definition.
     * 
     * @param user       a User instance, which should be an implementation class which
     *                   is handled by this Repostory implementation.
     * @param userUpdate a PreparedStatement initialised with SQL taken from the "update" SQL definition.
     * @exception SQLException
     *                   if an exception occurs while setting parameter values.
     */
    protected abstract void setUserForUpdateStatement(User user, 
                                                      PreparedStatement userUpdate)
        throws SQLException;

    /**
     * Opens a connection, handling exceptions.
     */
    private Connection openConnection()
    {
        try {
            return m_datasource.getConnection();
        }
        catch (SQLException sqle) {
            throw new CascadingRuntimeException(
                "An exception occurred getting a database connection.", sqle);
        }
    }

    /**
     * Closes a connection, handling exceptions.
     */
    private void closeConnection(Connection conn)
    {
        try {
            conn.close();
        }
        catch (SQLException sqle) {
            throw new CascadingRuntimeException(
                "An exception occurred closing a database connection.", sqle);
        }
    }

}    


