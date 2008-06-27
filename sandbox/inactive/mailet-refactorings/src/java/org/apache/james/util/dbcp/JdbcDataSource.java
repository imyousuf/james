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



package org.apache.james.util.dbcp;

import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.dbcp.BasicDataSource;
import javax.sql.DataSource;
import org.apache.mailet.MailetException;
import org.apache.mailet.MailetServiceJNDIRegistration;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * <p>
 * This is a reliable DataSource implementation, based on the pooling logic provided by <a
 * href="http://jakarta.apache.org/commons/dbcp.html">DBCP</a> and the configuration found in
 * Avalon's excalibur code.
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
 * <p>
 * These configuration settings are available:
 * <ul>
 * <li><b>driver</b> - The class name of the JDBC driver</li>
 * <li><b>dburl</b> - The JDBC URL for this connection</li>
 * <li><b>user</b> - The username to use for this connection</li>
 * <li><b>password</b> - The password to use for this connection</li>
 * <li><b>keep-alive</b> - The SQL query that will be used to validate connections from this pool before returning them to the caller.  If specified, this query <strong>MUST</strong> be an SQL SELECT statement that returns at least one row.</li>
 * <li><b>max</b> - The maximum number of active connections allowed in the pool. 0 means no limit. (default 2)</li>
 * <li><b>max_idle</b> - The maximum number of idle connections.  0 means no limit.  (default 0)</li>
 * <li><b>initial_size</b> -  The initial number of connections that are created when the pool is started. (default 0)</li>
 * <li><b>min_idle</b> -  The minimum number of active connections that can remain idle in the pool, without extra ones being created, or zero to create none. (default 0)</li>
 * <li><b>max_wait</b> -  The maximum number of milliseconds that the pool will wait (when there are no available connections) for a connection to be returned before throwing an exception, or -1 to wait indefinitely. (default -1)</li>
 * <li><b>testOnBorrow</b> -  The indication of whether objects will be validated before being borrowed from the pool. If the object fails to validate, it will be dropped from the pool, and we will attempt to borrow another.  (default true)</li>
 * <li><b>testOnReturn</b> -  The indication of whether objects will be validated before being returned to the pool. (default false)</li>
 * <li><b>testWhileIdle</b> -  The indication of whether objects will be validated by the idle object evictor (if any). If an object fails to validate, it will be dropped from the pool. (default false)</li>
 * <li><b>timeBetweenEvictionRunsMillis</b> -  The number of milliseconds to sleep between runs of the idle object evictor thread. When non-positive, no idle object evictor thread will be run. (default -1)</li>
 * <li><b>numTestsPerEvictionRun</b> -  The number of objects to examine during each run of the idle object evictor thread (if any). (default 3)</li>
 * <li><b>minEvictableIdleTimeMillis</b> -  The minimum amount of time an object may sit idle in the pool before it is eligable for eviction by the idle object evictor (if any). (default 1000 * 60 * 30)</li>
 * </ul>
 *
 * @version CVS $Revision$
 */
public class JdbcDataSource extends AbstractLogEnabled
    implements Configurable,
               Disposable,
               DataSourceComponent, DataSource {

    BasicDataSource source = null;
    //Jdbc2PoolDataSource source = null;
    //PoolingDataSource source = null;
    private String dsName;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration)
                   throws ConfigurationException {
        //Configure the DBCP
        try {
            dsName = configuration.getAttribute("name");
            String driver = configuration.getChild("driver").getValue(null);
            Class.forName(driver);

            String dburl = configuration.getChild("dburl").getValue(null);
            String user = configuration.getChild("user").getValue(null);
            String password = configuration.getChild("password").getValue(null);

            // This inner class extends DBCP's BasicDataSource, and
            // turns on validation (using Connection.isClosed()), so
            // that the pool can recover from a server outage.
            source = new BasicDataSource() {
                protected synchronized javax.sql.DataSource createDataSource()
                        throws SQLException {
                    if (dataSource != null) {
                        return (dataSource);
                    } else {
                        javax.sql.DataSource ds = super.createDataSource();
                        connectionPool.setTestOnBorrow(true);
                        connectionPool.setTestOnReturn(true);
                        return ds;
                    }
                }
            };

            source.setDriverClassName(driver);
            source.setUrl(dburl);
            source.setUsername(user);
            source.setPassword(password);
            source.setMaxActive(configuration.getChild("max").getValueAsInteger(2));
            source.setMaxIdle(configuration.getChild("max_idle").getValueAsInteger(0));
            source.setInitialSize(configuration.getChild("initial_size").getValueAsInteger(0));
            source.setMinIdle(configuration.getChild("min_idle").getValueAsInteger(0));
            //This is necessary, otherwise a connection could hang forever
            source.setMaxWait(configuration.getChild("max_wait").getValueAsInteger(5000));
            source.setValidationQuery(configuration.getChild("keep-alive").getValue(null));
            source.setTestOnBorrow(configuration.getChild("testOnBorrow").getValueAsBoolean(true));
            source.setTestOnReturn(configuration.getChild("testOnReturn").getValueAsBoolean(false));
            source.setTestWhileIdle(configuration.getChild("testWhileIdle").getValueAsBoolean(false));
            source.setTimeBetweenEvictionRunsMillis(configuration.getChild("timeBetweenEvictionRunsMillis").getValueAsInteger(-1));
            source.setNumTestsPerEvictionRun(configuration.getChild("numTestsPerEvictionRun").getValueAsInteger(3));
            source.setMinEvictableIdleTimeMillis(configuration.getChild("minEvictableIdleTimeMillis").getValueAsInteger(1000 * 30 * 60));

            //Unsupported
            //source.setLoginTimeout(configuration.getChild("login_timeout").getValueAsInteger(0));


            // DBCP uses a PrintWriter approach to logging.  This
            // Writer class will bridge between DBCP and Avalon
            // Logging. Unfortunately, DBCP 1.0 is clueless about the
            // concept of a log level.
            final java.io.Writer writer = new java.io.CharArrayWriter() {
                public void flush() {
                    // flush the stream to the log
                    if (JdbcDataSource.this.getLogger().isErrorEnabled()) {
                        JdbcDataSource.this.getLogger().error(toString());
                    }
                    reset();    // reset the contents for the next message
                }
            };

            source.setLogWriter(new PrintWriter(writer, true));

            // Extra debug for first cut
            getLogger().debug("max wait: " + source.getMaxWait());
            getLogger().debug("max idle: " + source.getMaxIdle());
            getLogger().debug("max active: " + source.getMaxActive());
            getLogger().debug("initial size: " + source.getInitialSize());
            getLogger().debug("TestOnBorrow: " + source.getTestOnBorrow());
            getLogger().debug("TestOnReturn: " + source.getTestOnReturn());
            getLogger().debug("TestWhileIdle: " + source.getTestWhileIdle());
            getLogger().debug("NumTestsPerEvictionRun(): " + source.getNumTestsPerEvictionRun());
            getLogger().debug("MinEvictableIdleTimeMillis(): " + source.getMinEvictableIdleTimeMillis());
            getLogger().debug("TimeBetweenEvictionRunsMillis(): " + source.getTimeBetweenEvictionRunsMillis());

            /*
            //Another sample that doesn't work
            GenericObjectPool connectionPool = new GenericObjectPool(null);
            ConnectionFactory connectionFactory =
                    new DriverManagerConnectionFactory(dburl, user, password);
            PoolableConnectionFactory poolableConnectionFactory =
                    new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
            PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
            source = dataSource;
            */

            /*
             As documented on the DBCP website, which is wrong
            DriverAdapterCPDS cpds = new DriverAdapterCPDS();
            cpds.setDriver(configuration.getChild("driver").getValue(null));
            cpds.setUrl(configuration.getChild("dburl").getValue(null));
            cpds.setUsername(configuration.getChild("user").getValue(null));
            cpds.setPassword(configuration.getChild("password").getValue(null));

            source = new Jdbc2PoolDataSource();
            source.setConnectionPoolDataSource(cpds);
            source.setDefaultMaxActive(10);
            source.setDefaultMaxWait(50);
            */


            //Get a connection and close it, just to test that we connected.
            source.getConnection().close();
        } catch (Exception e) {
            throw new ConfigurationException("Error configurable datasource", e);
        }
        
 
       
        
        try{
            MailetServiceJNDIRegistration.registerDataSource(dsName, this);
        }catch (MailetException e){
            throw new ConfigurationException("failed to register datasource ",e);
        }
    
    
        
        
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        //Close all database connections
        try {
            source.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        try{
            MailetServiceJNDIRegistration.deRegisterDataSource(dsName);
        }catch (MailetException e){
            //not much we care about if this fails
        }
    }

    /**
     * @see org.apache.mailet.DataSource#getConnection()
     */
    public Connection getConnection() throws SQLException {
        return source.getConnection();
    }

    /**
     * @param username
     * @param password
     * @return
     * @throws SQLException
     * @see javax.sql.DataSource#getConnection(java.lang.String, java.lang.String)
     */
    public Connection getConnection(String username, String password) throws SQLException {

        return source.getConnection(username, password);
    }

    /**
     * @return
     * @throws SQLException
     * @see javax.sql.DataSource#getLoginTimeout()
     */
    public int getLoginTimeout() throws SQLException {

        return source.getLoginTimeout();
    }

    /**
     * @return
     * @throws SQLException
     * @see javax.sql.DataSource#getLogWriter()
     */
    public PrintWriter getLogWriter() throws SQLException {

        return source.getLogWriter();
    }

    /**
     * @param seconds
     * @throws SQLException
     * @see javax.sql.DataSource#setLoginTimeout(int)
     */
    public void setLoginTimeout(int seconds) throws SQLException {

        source.setLoginTimeout(seconds);
    }

    /**
     * @param out
     * @throws SQLException
     * @see javax.sql.DataSource#setLogWriter(java.io.PrintWriter)
     */
    public void setLogWriter(PrintWriter out) throws SQLException {

        source.setLogWriter(out);
    }
}
