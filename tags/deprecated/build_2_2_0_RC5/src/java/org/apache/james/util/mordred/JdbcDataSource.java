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

package org.apache.james.util.mordred;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.ArrayList;

import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;


/**
 * <p>
 * This is a <b>reliable</b> DataSource implementation, based on the pooling logic written for <a
 * href="http://share.whichever.com/">Town</a> and the configuration found in Avalon's excalibur
 * code.
 * </p>
 *
 * <p>
 * This uses the normal <code>java.sql.Connection</code> object and
 * <code>java.sql.DriverManager</code>.  The Configuration is like this:
 * <pre>
 *   &lt;jdbc&gt;
 *     &lt;pool-controller min="<i>5</i>" max="<i>10</i>" connection-class="<i>my.overrided.ConnectionClass</i>"&gt;
 *       &lt;keep-alive&gt;select 1&lt;/keep-alive&gt;
 *     &lt;/pool-controller&gt;
 *     &lt;driver&gt;<i>com.database.jdbc.JdbcDriver</i>&lt;/driver&gt;
 *     &lt;dburl&gt;<i>jdbc:driver://host/mydb</i>&lt;/dburl&gt;
 *     &lt;user&gt;<i>username</i>&lt;/user&gt;
 *     &lt;password&gt;<i>password</i>&lt;/password&gt;
 *   &lt;/jdbc&gt;
 * </pre>
 * </p>
 *
 * @version CVS $Revision: 1.18.4.8 $
 * @since 4.0
 */
public class JdbcDataSource extends AbstractLogEnabled
    implements Configurable,
               Runnable,
               Disposable,
               DataSourceComponent {
    // The limit that an active connection can be running
    public static final long ACTIVE_CONN_TIME_LIMIT = 60000; // (one minute)
    public static final long ACTIVE_CONN_HARD_TIME_LIMIT = 2*ACTIVE_CONN_TIME_LIMIT;
    // How long before you kill off a connection due to inactivity
    public static final long CONN_IDLE_LIMIT        = 600000; // (10 minutes)
    private static final boolean DEEP_DEBUG         = false;
    private static int total_served                 = 0;
    // This is a temporary variable used to track how many active threads
    // are in createConnection().  This is to prevent to many connections
    // from being opened at once.
    private int connCreationsInProgress             = 0;
    // The error message is the conn pooler cannot serve connections for whatever reason
    private String connErrorMessage                 = null;
    // the last time a connection was created...
    private long connLastCreated                    = 0;
    // connection number for like of this broker
    private int connectionCount;
    // Driver class
    private String jdbcDriver;
    // Password to login to database
    private String jdbcPassword;
    // Server to connect to database (this really is the jdbc URL)
    private String jdbcURL;
    // Username to login to database
    private String jdbcUsername;
    // Maximum number of connections to have open at any point
    private int maxConn                             = 0;
    // collection of connection objects
    private ArrayList pool;
    // Thread that checks for dead/aged connections and removes them from pool
    private Thread reaper;
    // Flag to indicate whether reaper thread should run
    private boolean reaperActive                    = false;
    // a SQL command to execute to see if the connection is still ok
    private String verifyConnSQL;

    /**
     * Implements the ConnDefinition behavior when a connection is needed. Checks the pool of
     * connections to see if there is one available.  If there is not and we are below the max
     * number of connections limit, it tries to create another connection.  It retries this 10
     * times until a connection is available or can be created
     *
     * @return java.sql.Connection
     * @throws SQLException Document throws!
     */
    public Connection getConnection() throws SQLException {
        //If the conn definition has a fatal connection problem, need to return that error
        if(connErrorMessage != null) {
            throw new SQLException(connErrorMessage);
        }
        //Look through our list of open connections right now, starting from beginning.
        //If we find one, book it.
        int count                                   = total_served++;
        if(DEEP_DEBUG) {
            StringBuffer deepDebugBuffer =
                new StringBuffer(128)
                        .append((new java.util.Date()).toString())
                        .append(" trying to get a connection (")
                        .append(count)
                        .append(")");
            System.out.println(deepDebugBuffer.toString());
        }
        for(int attempts = 1; attempts <= 100; attempts++) {
            synchronized(pool) {
                for(int i = 0; i < pool.size(); i++) {
                    PoolConnEntry entry = (PoolConnEntry)pool.get(i);
                    //Set the appropriate flags to make this connection
                    //marked as in use
                    try {
                        if(entry.lock()) {
                            if(DEEP_DEBUG) {
                                StringBuffer deepDebugBuffer =
                                    new StringBuffer(128)
                                            .append((new java.util.Date()).toString())
                                            .append(" return a connection (")
                                            .append(count)
                                            .append(")");
                                System.out.println(deepDebugBuffer.toString());
                            }
                            return entry;
                        }
                    } catch(SQLException se) {
                        //Somehow a closed connection appeared in our pool.
                        //Remove it immediately.
                        finalizeEntry(entry);
                        continue;
                    }
                    //we were unable to get a lock on this entry.. so continue through list
                } //loop through existing connections
                //If we have 0, create another
                if(DEEP_DEBUG) {
                    System.out.println(pool.size());
                }
                try {
                    if(pool.size() == 0) {
                        //create a connection
                        PoolConnEntry entry = createConn();
                        if(entry != null) {
                            if(DEEP_DEBUG) {
                                StringBuffer deepDebugBuffer =
                                    new StringBuffer(128)
                                            .append((new java.util.Date()).toString())
                                            .append(" returning new connection (")
                                            .append(count)
                                            .append(")");
                                System.out.println(deepDebugBuffer.toString());
                            }
                            return entry;
                        }
                        //looks like a connection was already created
                    } else {
                        //Since we didn't find one, and we have < max connections, then consider whether
                        //  we create another
                        //if we've hit the 3rd attempt without getting a connection,
                        //  let's create another to anticipate a slow down
                        if((attempts == 2) && (pool.size() < maxConn || maxConn == 0)) {
                            PoolConnEntry entry = createConn();
                            if(entry != null) {
                                if(DEEP_DEBUG) {
                                    StringBuffer deepDebugBuffer =
                                        new StringBuffer(32)
                                                .append(" returning new connection (")
                                                .append(count)
                                                .append(")");
                                    System.out.println(deepDebugBuffer.toString());
                                }
                                return entry;
                            } else {
                                attempts = 1;
                            }
                        }
                    }
                } catch(SQLException sqle) {
                    //Ignore... error creating the connection
                    StringWriter sout = new StringWriter();
                    PrintWriter pout  = new PrintWriter(sout, true);
                    pout.println("Error creating connection: ");
                    sqle.printStackTrace(pout);
                    if (getLogger().isErrorEnabled()) {
                        getLogger().error(sout.toString());
                    }
                }
            }
            //otherwise sleep 50ms 10 times, then create a connection
            try {
                Thread.currentThread().sleep(50);
            } catch(InterruptedException ie) {
            }
        }
        // Give up... no connections available
        throw new SQLException("Giving up... no connections available.");
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration)
                   throws ConfigurationException {
        try {
            jdbcDriver    = configuration.getChild("driver").getValue(null);
            jdbcURL       = configuration.getChild("dburl").getValue(null);
            jdbcUsername  = configuration.getChild("user").getValue(null);
            jdbcPassword  = configuration.getChild("password").getValue(null);
            maxConn       = configuration.getChild("max").getValueAsInteger(2);
            //logfilename?
            verifyConnSQL = configuration.getChild("keep-alive").getValue(null);
            //Not support from Town: logfilename
            //Not supporting from Excalibur: pool-controller, min, auto-commit, oradb, connection-class
            if(jdbcDriver == null) {
                throw new ConfigurationException("You need to specify a valid driver, e.g., <driver>my.class</driver>");
            }
            try {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Loading new driver: " + jdbcDriver);
                }
                // TODO: Figure out why this breaks when we change the Class.forName to
                //       a loadClass method call on the class loader.
                // DO NOT MESS WITH THIS UNLESS YOU ARE WILLING TO TEST
                // AND FIX THE PROBLEMS!
                Class.forName(jdbcDriver, true, Thread.currentThread().getContextClassLoader());
                // These variations do NOT work:
                // getClass().getClassLoader().loadClass(jdbcDriver);                    -- DON'T USE -- BROKEN!!
                // Thread.currentThread().getContextClassLoader().loadClass(jdbcDriver); -- DON'T USE -- BROKEN!!
            } catch(ClassNotFoundException cnfe) {
                StringBuffer exceptionBuffer =
                    new StringBuffer(128)
                            .append("'")
                            .append(jdbcDriver)
                            .append("' could not be found in classloader.  Please specify a valid JDBC driver");
                String exceptionMessage = exceptionBuffer.toString();
                getLogger().error(exceptionMessage);
                throw new ConfigurationException(exceptionMessage);
            }
            if(jdbcURL == null) {
                throw new ConfigurationException("You need to specify a valid JDBC connection string, e.g., <dburl>jdbc:driver:database</dburl>");
            }
            if(maxConn < 0) {
                throw new ConfigurationException("Maximum number of connections specified must be at least 1 (0 means no limit).");
            }
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Starting connection pooler");
                getLogger().debug("driver = " + jdbcDriver);
                getLogger().debug("dburl = " + jdbcURL);
                getLogger().debug("username = " + jdbcUsername);
                //We don't show the password
                getLogger().debug("max connections = " + maxConn);
            }
            pool         = new ArrayList();
            reaperActive = true;
            reaper       = new Thread(this);
            reaper.setDaemon(true);
            reaper.start();
        } catch(ConfigurationException ce) {
            //Let this pass through...
            throw ce;
        }
         catch(Exception e) {
            throw new ConfigurationException("Error configuring JdbcDataSource", e);
        }
    }

    /**
     * The dispose operation is called at the end of a components lifecycle.
     * Cleans up all JDBC connections.
     *
     * @throws Exception if an error is encountered during shutdown
     */
    public void dispose() {
        // Stop the background monitoring thread
        if(reaper != null) {
            reaperActive = false;
            //In case it's sleeping, help it quit faster
            reaper.interrupt();
            reaper = null;
        }
        // The various entries will finalize themselves once the reference
        // is removed, so no need to do it here
    }

    /**
     * Close all connections.  The connection pooler will recreate these connections if something
     * starts requesting them again.
     *
     * @deprecated This was left over code from Town... but not exposed in Avalon.
     */
    public void killAllConnections() {
        //Just remove the references to all the connections... this will cause them to get
        // finalized before very long. (not an instant shutdown, but that's ok).
        synchronized (pool) { pool.clear(); }
    }

    /**
     * Implements the ConnDefinition behavior when something bad has happened to a connection. If a
     * sql command was provided in the properties file, it will run this and attempt to determine
     * whether the connection is still valid.  If it is, it recycles this connection back into the
     * pool.  If it is not, it closes the connection.
     *
     * @param entry the connection that had problems
     * @deprecated - No longer used in the new approach.
     */
    public void killConnection(PoolConnEntry entry) {
        if(entry != null) {
            // if we were provided SQL to test the connection with, we will use
            // this and possibly just release the connection after clearing warnings
            if(verifyConnSQL != null) {
                try {
                    // Test this connection
                    java.sql.Statement stmt = null;
                    try {
                        stmt = entry.createStatement();
                        stmt.execute(verifyConnSQL);
                    } finally {
                        try {
                            if (stmt != null) {
                                stmt.close();
                            }
                        } catch (SQLException sqle) {
                            // Failure to close ignored on test connection
                        }
                    }
                    // Passed test... recycle the entry
                    entry.unlock();
                } catch(SQLException e1) {
                    // Failed test... close the entry
                    finalizeEntry(entry);
                }
            } else {
                // No SQL was provided... we have to kill this entry to be sure
                finalizeEntry(entry);
            }
            return;
        } else {
            if (getLogger().isWarnEnabled()) {
                getLogger().warn("----> Could not find connection to kill!!!");
            }
            return;
        }
    }

    /**
     * Implements the ConnDefinition behavior when a connection is no longer needed. This resets
     * flags on the wrapper of the connection to allow others to use this connection.
     *
     * @param entry
     */
    public void releaseConnection(PoolConnEntry entry) {
        //PoolConnEntry entry = findEntry(conn);
        if(entry != null) {
            entry.unlock();
            return;
        } else {
            if (getLogger().isWarnEnabled()) {
                getLogger().warn("----> Could not find the connection to free!!!");
            }
            return;
        }
    }

    /**
     * Background thread that checks if there are fewer connections open than minConn specifies,
     * and checks whether connections have been checked out for too long, killing them.
     */
    public void run() {
        try {
            while(reaperActive) {
                synchronized(pool) {
                    for(int i = 0; i < pool.size(); i++) try {
                        PoolConnEntry entry = (PoolConnEntry)pool.get(i);
                        long age            = System.currentTimeMillis() - entry.getLastActivity();
                        synchronized(entry) {
                            if((entry.getStatus() == PoolConnEntry.ACTIVE) &&
                               (age > ACTIVE_CONN_HARD_TIME_LIMIT)) {
                                StringBuffer logBuffer =
                                    new StringBuffer(128)
                                            .append(" ***** connection ")
                                            .append(entry.getId())
                                            .append(" is way too old: ")
                                            .append(age)
                                            .append(" > ")
                                            .append(ACTIVE_CONN_HARD_TIME_LIMIT)
                                            .append(" and will be closed.");
                                getLogger().info(logBuffer.toString());
                                // This connection is way too old...
                                // kill it no matter what
                                finalizeEntry(entry);
                                continue;
                            }
                            if((entry.getStatus() == PoolConnEntry.ACTIVE) &&
                               (age > ACTIVE_CONN_TIME_LIMIT)) {
                                StringBuffer logBuffer =
                                    new StringBuffer(128)
                                            .append(" ***** connection ")
                                            .append(entry.getId())
                                            .append(" is way too old: ")
                                            .append(age)
                                            .append(" > ")
                                            .append(ACTIVE_CONN_TIME_LIMIT);
                                getLogger().info(logBuffer.toString());
                                // This connection is way too old...
                                // just log it for now.
                                continue;
                            }
                            if((entry.getStatus() == PoolConnEntry.AVAILABLE) &&
                               (age > CONN_IDLE_LIMIT)) {
                                //We've got a connection that's too old... kill it
                                finalizeEntry(entry);
                                continue;
                            }
                        }
                    }
                    catch (Throwable ex)
                    {
                        StringWriter sout = new StringWriter();
                        PrintWriter pout = new PrintWriter(sout, true);
                        pout.println("Reaper Error: ");
                        ex.printStackTrace(pout);
                        if (getLogger().isErrorEnabled()) {
                            getLogger().error(sout.toString());
                        }
                    }
                }
                try {
                    // Check for activity every 5 seconds
                    Thread.sleep(5000L);
                } catch(InterruptedException ex) {
                }
            }
        } finally {
            Thread.currentThread().interrupted();
        }
    }

    protected void debug(String message) {
        getLogger().debug(message);
    }

    protected void info(String message) {
        getLogger().info(message);
    }

    /*
     * This is a real hack, but oh well for now
     */
    protected void warn(String message) {
        getLogger().warn(message);
    }

    /**
     * Creates a new connection as per these properties, adds it to the pool, and logs the creation.
     *
     * @return PoolConnEntry the new connection wrapped as an entry
     * @throws SQLException
     */
    private PoolConnEntry createConn() throws SQLException {
        PoolConnEntry entry = null;
        synchronized(pool) {
            if(connCreationsInProgress > 0) {
                //We are already creating one in another place
                return null;
            }
            long now = System.currentTimeMillis();
            if((now - connLastCreated) < (1000 * pool.size())) {
                //We don't want to scale up too quickly...
                if(DEEP_DEBUG) {
                    System.err.println("We don't want to scale up too quickly");
                }
                return null;
            }
            if((maxConn == 0) || (pool.size() < maxConn)) {
                connCreationsInProgress++;
                connLastCreated = now;
            } else {
                // We've already hit a limit... fail silently
                if (getLogger().isDebugEnabled())
                {
                    StringBuffer logBuffer =
                        new StringBuffer(128)
                                .append("Connection limit hit... ")
                                .append(pool.size())
                                .append(" in pool and ")
                                .append(connCreationsInProgress)
                                .append(" + on the way.");
                    getLogger().debug(logBuffer.toString());
                }
                return null;
            }
            try {
                entry = new PoolConnEntry(this,
                                          java.sql.DriverManager.getConnection(jdbcURL, jdbcUsername,
                                                                               jdbcPassword),
                                          ++connectionCount);
                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug("Opening connection " + entry);
                }
                entry.lock();
                pool.add(entry);
                return entry;
            } catch(SQLException sqle) {
                //Shouldn't ever happen, but it did, just return null.
                // Exception from DriverManager.getConnection() - log it, and return null
                StringWriter sout = new StringWriter();
                PrintWriter pout = new PrintWriter(sout, true);
                pout.println("Error creating connection: ");
                sqle.printStackTrace(pout);
                if (getLogger().isErrorEnabled()) {
                    getLogger().error(sout.toString());
                }
                return null;
            } finally {
                    connCreationsInProgress--;
            }
        }
    }

    /**
     * Closes a connection and removes it from the pool.
     *
     * @param entry entry
     */
    private void finalizeEntry(PoolConnEntry entry) {
        synchronized(pool) {
            try {
                entry.finalize();
            } catch(Exception fe) {
            }
            pool.remove(entry);
        }
    }
}
