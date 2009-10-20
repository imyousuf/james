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

package org.apache.james.smtpserver.integration;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.dnsservice.util.NetMatcher;
import org.apache.james.api.protocol.LogEnabled;
import org.apache.james.services.FileSystem;
import org.apache.james.smtpserver.protocol.core.fastfail.AbstractGreylistHandler;
import org.apache.james.util.TimeConverter;
import org.apache.james.util.sql.JDBCUtil;
import org.apache.james.util.sql.SqlResources;

/**
 * GreylistHandler which can be used to activate Greylisting
 */
public class JDBCGreylistHandler extends AbstractGreylistHandler implements LogEnabled{

    
    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(JDBCGreylistHandler.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log serviceLog = FALLBACK_LOG;

    private DataSourceSelector datasources = null;

    private DataSourceComponent datasource = null;

    private FileSystem fileSystem = null;

    private String selectQuery;

    private String insertQuery;

    private String deleteQuery;

    private String deleteAutoWhiteListQuery;

    private String updateQuery;

    /**
     * Contains all of the sql strings for this component.
     */
    private SqlResources sqlQueries = new SqlResources();

    /**
     * The sqlFileUrl
     */
    private String sqlFileUrl;

    /**
     * Holds value of property sqlParameters.
     */
    private Map<String, String> sqlParameters = new HashMap<String, String>();

    /**
     * The repositoryPath
     */
    private String repositoryPath;
    
    
    /**
     * Gets the file system service.
     * @return the fileSystem
     */
    public final FileSystem getFileSystem() {
        return fileSystem;
    }
    
    /**
     * Sets the filesystem service
     * 
     * @param system The filesystem service
     */
    @Resource(name="filesystem")
    public void setFileSystem(FileSystem system) {
        this.fileSystem = system;
    }
    
    /**
     * @return the datasources
     */
    public final DataSourceSelector getDatasources() {
        return datasources;
    }

    /**
     * Set the datasources.
     * 
     * @param datasources
     *            The datasources
     */
    @Resource(name="database-connections")
    public void setDataSources(DataSourceSelector datasources) {
        this.datasources = datasources;
    }


    /**
     * Set the sqlFileUrl to use for getting the sqlRessource.xml file
     * 
     * @param sqlFileUrl
     *            The fileUrl
     */
    public void setSqlFileUrl(String sqlFileUrl) {
        this.sqlFileUrl = sqlFileUrl;
    } 

    /**
     * Setup the temporary blocking time
     * 
     * @param tempBlockTime
     *            The temporary blocking time 
     */
    public void setTempBlockTime(String tempBlockTime) {
       setTempBlockTime(TimeConverter.getMilliSeconds(tempBlockTime));
    }

    /**
     * Setup the autowhitelist lifetime for which we should whitelist a triplet.
     * After this lifetime the record will be deleted
     * 
     * @param autoWhiteListLifeTime
     *            The lifeTime 
     */
    public void setAutoWhiteListLifeTime(String autoWhiteListLifeTime) {
        setAutoWhiteListLifeTime(TimeConverter.getMilliSeconds(autoWhiteListLifeTime));
    }

    /**
     * Set up the liftime of only once seen triplet. After this liftime the
     * record will be deleted
     * 
     * @param unseenLifeTime
     *            The lifetime 
     */
    public void setUnseenLifeTime(String unseenLifeTime) {
        setUnseenLifeTime(TimeConverter.getMilliSeconds(unseenLifeTime));
    }
    
    /**
     * @see org.apache.james.smtpserver.protocol.core.fastfail.AbstractGreylistHandler#configure(org.apache.commons.configuration.Configuration)
     */
    @SuppressWarnings("unchecked")
	public void configure(Configuration handlerConfiguration) throws ConfigurationException {
	    try {
            setTempBlockTime(handlerConfiguration.getString("tempBlockTime"));
        } catch (NumberFormatException e) {
           throw new ConfigurationException(e.getMessage());
        }
       
    
      
        try {
            setAutoWhiteListLifeTime(handlerConfiguration.getString("autoWhiteListLifeTime"));
        } catch (NumberFormatException e) {
            throw new ConfigurationException(e.getMessage());
        }
       

        try {
            setUnseenLifeTime(handlerConfiguration.getString("unseenLifeTime"));
        } catch (NumberFormatException e) {
            throw new ConfigurationException(e.getMessage());
        }
        Collection<String> nets  = handlerConfiguration.getList("whitelistedNetworks");
        if (nets != null) {

            if (nets != null) {
                setWhiteListedNetworks( new NetMatcher(nets,getDNSService()));
                serviceLog.info("Whitelisted addresses: " + getWhiteListedNetworks().toString());
            }
        }    	
        String configRepositoryPath = handlerConfiguration.getString("repositoryPath", null);
        if (configRepositoryPath != null) {
            setRepositoryPath(configRepositoryPath);
        } else {
            throw new ConfigurationException("repositoryPath is not configured");
        }

        // Get the SQL file location
        String sFile = handlerConfiguration.getString("sqlFile",null);
        if (sFile != null) {
            
        	setSqlFileUrl(sFile);
            
            if (!sqlFileUrl.startsWith("file://")) {
                throw new ConfigurationException(
                    "Malformed sqlFile - Must be of the format \"file://<filename>\".");
            }
        } else {
            throw new ConfigurationException("sqlFile is not configured");
        }
    }

    @PostConstruct
    public void init() throws Exception {
    	 try {
 			setDataSource(initDataSource(repositoryPath));
 			initSqlQueries(datasource.getConnection(), sqlFileUrl);
 		        
 		    // create table if not exist
 		    createTable("greyListTableName", "createGreyListTable");
 		} catch (Exception e) {
 			throw new RuntimeException("Unable to init datasource",e);
 		}
    }
    
    /**
     * Set the repositoryPath to use
     * 
     * @param repositoryPath
     *            The repositoryPath
     */
    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    /**
     * Set the datasource
     * 
     * @param datasource
     *            the datasource
     */
    public void setDataSource(DataSourceComponent datasource) {
        this.datasource = datasource;
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.fastfail.AbstractGreylistHandler#getGreyListData(java.lang.String, java.lang.String, java.lang.String)
     */
    protected Iterator<String> getGreyListData(String ipAddress,
        String sender, String recip) throws SQLException {
        Collection<String> data = new ArrayList<String>(2);
        PreparedStatement mappingStmt = null;
        Connection conn = datasource.getConnection();
        try {
            mappingStmt = conn.prepareStatement(selectQuery);
            ResultSet mappingRS = null;
            try {
                mappingStmt.setString(1, ipAddress);
                mappingStmt.setString(2, sender);
                mappingStmt.setString(3, recip);
                mappingRS = mappingStmt.executeQuery();

                if (mappingRS.next()) {
                    data.add(String.valueOf(mappingRS.getTimestamp(1).getTime()));
                    data.add(String.valueOf(mappingRS.getInt(2)));
                }
            } finally {
                theJDBCUtil.closeJDBCResultSet(mappingRS);
            }
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
        return data.iterator();
    }

    
    /**
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.protocol.core.fastfail.AbstractGreylistHandler#insertTriplet(java.lang.String, java.lang.String, java.lang.String, int, long)
     */
    protected void insertTriplet(String ipAddress,
        String sender, String recip, int count, long createTime)
        throws SQLException {
        Connection conn = datasource.getConnection();

        PreparedStatement mappingStmt = null;

        try {
            mappingStmt = conn.prepareStatement(insertQuery);

            mappingStmt.setString(1, ipAddress);
            mappingStmt.setString(2, sender);
            mappingStmt.setString(3, recip);
            mappingStmt.setInt(4, count);
            mappingStmt.setTimestamp(5, new Timestamp(createTime));
            mappingStmt.executeUpdate();
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.fastfail.AbstractGreylistHandler#updateTriplet(java.lang.String, java.lang.String, java.lang.String, int, long)
     */
    protected void updateTriplet(String ipAddress,
        String sender, String recip, int count, long time)
        throws SQLException {
        Connection conn = datasource.getConnection();
        PreparedStatement mappingStmt = null;

        try {
            mappingStmt = conn.prepareStatement(updateQuery);
            mappingStmt.setTimestamp(1, new Timestamp(time));
            mappingStmt.setInt(2, (count + 1));
            mappingStmt.setString(3, ipAddress);
            mappingStmt.setString(4, sender);
            mappingStmt.setString(5, recip);
            mappingStmt.executeUpdate();
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * Init the dataSource
     * 
     * @param repositoryPath
     *            The repositoryPath
     * @return dataSource The DataSourceComponent
     * @throws ServiceException
     * @throws SQLException
     */
    private DataSourceComponent initDataSource(String repositoryPath)
        throws ServiceException, SQLException {

        int stindex = repositoryPath.indexOf("://") + 3;
        String datasourceName = repositoryPath.substring(stindex);

        return (DataSourceComponent) datasources.select(datasourceName);
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.fastfail.AbstractGreylistHandler#cleanupAutoWhiteListGreyList(long)
     */
    protected void cleanupAutoWhiteListGreyList(long time)
        throws SQLException {
        PreparedStatement mappingStmt = null;
        Connection conn = datasource.getConnection();

        try {
            mappingStmt = conn.prepareStatement(deleteAutoWhiteListQuery);

            mappingStmt.setTimestamp(1, new Timestamp(time));

            mappingStmt.executeUpdate();
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.fastfail.AbstractGreylistHandler#cleanupGreyList(long)
     */
    protected void cleanupGreyList(long time)
        throws SQLException {
        Connection conn = datasource.getConnection();

        PreparedStatement mappingStmt = null;

        try {
            mappingStmt = conn.prepareStatement(deleteQuery);

            mappingStmt.setTimestamp(1, new Timestamp(time));

            mappingStmt.executeUpdate();
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * The JDBCUtil helper class
     */
    private final JDBCUtil theJDBCUtil = new JDBCUtil() {
        protected void delegatedLog(String logString) {
            serviceLog.debug("JDBCVirtualUserTable: " + logString);
        }
    };

    /**
     * Initializes the sql query environment from the SqlResources file. Will
     * look for conf/sqlResources.xml.
     * 
     * @param conn
     *            The connection for accessing the database
     * @param sqlFileUrl
     *            The url which we use to get the sql file
     * @throws Exception
     *             If any error occurs
     */
    private void initSqlQueries(Connection conn, String sqlFileUrl)
        throws Exception {
        try {

            File sqlFile = null;
    
            try {
                sqlFile = fileSystem.getFile(sqlFileUrl);
                sqlFileUrl = null;
            } catch (Exception e) {
                serviceLog.fatal(e.getMessage(), e);
                throw e;
            }

            sqlQueries.init(sqlFile.getCanonicalFile(), "GreyList", conn, sqlParameters);

            selectQuery = sqlQueries.getSqlString("selectQuery", true);
            insertQuery = sqlQueries.getSqlString("insertQuery", true);
            deleteQuery = sqlQueries.getSqlString("deleteQuery", true);
            deleteAutoWhiteListQuery = sqlQueries.getSqlString("deleteAutoWhitelistQuery", true);
            updateQuery = sqlQueries.getSqlString("updateQuery", true);

        } finally {
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }

    /**
     * Create the table if not exists.
     * 
     * @param conn
     *            The connection
     * @param tableNameSqlStringName
     *            The tableSqlname
     * @param createSqlStringName
     *            The createSqlname
     * @return true or false
     * @throws SQLException
     */
    private boolean createTable(String tableNameSqlStringName,
    String createSqlStringName) throws SQLException {
        Connection conn = datasource.getConnection();
        String tableName = sqlQueries.getSqlString(tableNameSqlStringName, true);

        DatabaseMetaData dbMetaData = conn.getMetaData();

        // Try UPPER, lower, and MixedCase, to see if the table is there.
        if (theJDBCUtil.tableExists(dbMetaData, tableName)) {
            return false;
        }

        PreparedStatement createStatement = null;

        try {
            createStatement = conn.prepareStatement(sqlQueries.getSqlString(createSqlStringName, true));
            createStatement.execute();

            StringBuilder logBuffer = null;
            logBuffer = new StringBuilder(64).append("Created table '").append(tableName)
            .append("' using sqlResources string '")
            .append(createSqlStringName).append("'.");
            serviceLog.info(logBuffer.toString());

        } finally {
            theJDBCUtil.closeJDBCStatement(createStatement);
        }
        return true;
    }

    /**
     * @see org.apache.james.api.protocol.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.serviceLog = log;
    }
}
