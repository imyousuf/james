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

package org.apache.james.util.dbcp;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Vector;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.InitialContext;

//import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
//import org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS;
//import org.apache.commons.pool.impl.GenericObjectPool;
//import org.apache.commons.dbcp.ConnectionFactory;
//import org.apache.commons.dbcp.DriverManagerConnectionFactory;
//import org.apache.commons.dbcp.PoolingDataSource;
//import org.apache.commons.dbcp.PoolableConnectionFactory;


import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

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
 * </ul>
 *
 * @version CVS $Revision: 1.1.2.3 $
 */
public class JdbcDataSource extends AbstractLogEnabled
    implements Configurable,
               Disposable,
               DataSourceComponent {

    BasicDataSource source = null;
    //Jdbc2PoolDataSource source = null;
    //PoolingDataSource source = null;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration)
                   throws ConfigurationException {
        //Configure the DBCP
        try {
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
            source.setValidationQuery(configuration.getChild("keep-alive").getValue(null));
            //Unsupported
            //source.setLoginTimeout(configuration.getChild("login_timeout").getValueAsInteger(0));

            //This is necessary, otherwise a connection could hang forever
            source.setMaxWait(configuration.getChild("max_wait").getValueAsInteger(5000));

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

            /*
            Extra debug for first cut
            System.err.println("max wait: " + source.getMaxWait());
            System.err.println("max idle: " + source.getMaxIdle());
            System.err.println("max active: " + source.getMaxActive());
            System.err.println("getRemoveAbandonedTimeout: " + source.getRemoveAbandonedTimeout());
            System.err.println("getRemoveAbandoned(): " + source.getRemoveAbandoned());
            System.err.println("getNumTestsPerEvictionRun(): " + source.getNumTestsPerEvictionRun());
            System.err.println("getMinEvictableIdleTimeMillis(): " + source.getMinEvictableIdleTimeMillis());
            System.err.println("getTimeBetweenEvictionRunsMillis(): " + source.getTimeBetweenEvictionRunsMillis());
            */

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
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#dispose()
     */
    public void dispose() {
        //Close all database connections
        try {
            source.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    /**
     *
     */
    public Connection getConnection() throws SQLException {
        return source.getConnection();
    }
}
