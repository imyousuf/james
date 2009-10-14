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

package org.apache.james.smtpserver.protocol.core.filter.fastfail;

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

import javax.annotation.Resource;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.dnsservice.util.NetMatcher;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.services.FileSystem;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.james.smtpserver.protocol.hook.RcptHook;
import org.apache.james.socket.configuration.Configurable;
import org.apache.james.socket.shared.LogEnabled;
import org.apache.james.util.TimeConverter;
import org.apache.james.util.sql.JDBCUtil;
import org.apache.james.util.sql.SqlResources;
import org.apache.mailet.MailAddress;

/**
 * GreylistHandler which can be used to activate Greylisting
 */
public class GreylistHandler implements LogEnabled, RcptHook, Configurable {

    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(GreylistHandler.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log serviceLog = FALLBACK_LOG;

    private DataSourceSelector datasources = null;

    private DataSourceComponent datasource = null;

    private FileSystem fileSystem = null;

    /** 1 hour */
    private long tempBlockTime = 3600000;

    /** 36 days */
    private long autoWhiteListLifeTime = 3110400000L;

    /** 4 hours */
    private long unseenLifeTime = 14400000;

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

    private DNSService dnsService;

    private NetMatcher wNetworks;


    /**
     * Sets the service log.
     * Where available, a context sensitive log should be used.
     * @param Log not null
     */
    public void setLog(Log log) {
        this.serviceLog = log;
    }
    
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
     * Gets the DNS service.
     * @return the dnsService
     */
    public final DNSService getDNSService() {
        return dnsService;
    }

    /**
     * Sets the DNS service.
     * @param dnsService the dnsService to set
     */
    @Resource(name="dnsserver")
    public final void setDNSService(DNSService dnsService) {
        this.dnsService = dnsService;
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

        Collection<String> nets  = handlerConfiguration.getList("whitelistedNetworks");
        if (nets != null) {

            if (nets != null) {
                wNetworks = new NetMatcher(nets,dnsService);
                serviceLog.info("Whitelisted addresses: " + wNetworks.toString());
            }
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
       
    }
    
    /**
     * Set the repositoryPath to use
     * 
     * @param repositoryPath
     *            The repositoryPath
     */
    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
        
        try {
			setDataSource(initDataSource(repositoryPath));
			initSqlQueries(datasource.getConnection(), sqlFileUrl);
		        
		    // create table if not exist
		    createTable(datasource.getConnection(), "greyListTableName", "createGreyListTable");
		} catch (Exception e) {
			throw new RuntimeException("Unable to init datasource",e);
		}
      
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
     * Setup the temporary blocking time
     * 
     * @param tempBlockTime
     *            The temporary blocking time 
     */
    public void setTempBlockTime(String tempBlockTime) {
        this.tempBlockTime = TimeConverter.getMilliSeconds(tempBlockTime);
    }

    /**
     * Setup the autowhitelist lifetime for which we should whitelist a triplet.
     * After this lifetime the record will be deleted
     * 
     * @param autoWhiteListLifeTime
     *            The lifeTime 
     */
    public void setAutoWhiteListLifeTime(String autoWhiteListLifeTime) {
        this.autoWhiteListLifeTime = TimeConverter.getMilliSeconds(autoWhiteListLifeTime);
    }

    /**
     * Set up the liftime of only once seen triplet. After this liftime the
     * record will be deleted
     * 
     * @param unseenLifeTime
     *            The lifetime 
     */
    public void setUnseenLifeTime(String unseenLifeTime) {
        this.unseenLifeTime = TimeConverter.getMilliSeconds(unseenLifeTime);
    }

    private HookResult doGreyListCheck(SMTPSession session, MailAddress senderAddress, MailAddress recipAddress) {
        String recip = "";
        String sender = "";

        if (recipAddress != null) recip = recipAddress.toString();
        if (senderAddress != null) sender = senderAddress.toString();
    
        long time = System.currentTimeMillis();
        String ipAddress = session.getRemoteIPAddress();
        
        try {
            long createTimeStamp = 0;
            int count = 0;
            
            // get the timestamp when he triplet was last seen
            Iterator<String> data = getGreyListData(datasource.getConnection(), ipAddress, sender, recip);
            
            if (data.hasNext()) {
                createTimeStamp = Long.parseLong(data.next());
                count = Integer.parseInt(data.next());
            }
            
            session.getLogger().debug("Triplet " + ipAddress + " | " + sender + " | " + recip  +" -> TimeStamp: " + createTimeStamp);


            // if the timestamp is bigger as 0 we have allready a triplet stored
            if (createTimeStamp > 0) {
                long acceptTime = createTimeStamp + tempBlockTime;
        
                if ((time < acceptTime) && (count == 0)) {
                    return new HookResult(HookReturnCode.DENYSOFT, SMTPRetCode.LOCAL_ERROR, DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.NETWORK_DIR_SERVER) 
                        + " Temporary rejected: Reconnect to fast. Please try again later");
                } else {
                    
                    session.getLogger().debug("Update triplet " + ipAddress + " | " + sender + " | " + recip + " -> timestamp: " + time);
                    
                    // update the triplet..
                    updateTriplet(datasource.getConnection(), ipAddress, sender, recip, count, time);

                }
            } else {
                session.getLogger().debug("New triplet " + ipAddress + " | " + sender + " | " + recip );
           
                // insert a new triplet
                insertTriplet(datasource.getConnection(), ipAddress, sender, recip, count, time);
      
                // Tempory block on new triplet!
                return new HookResult(HookReturnCode.DENYSOFT, SMTPRetCode.LOCAL_ERROR, DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.NETWORK_DIR_SERVER) 
                    + " Temporary rejected: Please try again later");
            }

            // some kind of random cleanup process
            if (Math.random() > 0.99) {
                // cleanup old entries
            
                session.getLogger().debug("Delete old entries");
            
                cleanupAutoWhiteListGreyList(datasource.getConnection(),(time - autoWhiteListLifeTime));
                cleanupGreyList(datasource.getConnection(), (time - unseenLifeTime));
            }

        } catch (SQLException e) {
            // just log the exception
            session.getLogger().error("Error on SQLquery: " + e.getMessage());
        }
        return new HookResult(HookReturnCode.DECLINED);
    }

    /**
     * Get all necessary data for greylisting based on provided triplet
     * 
     * @param conn
     *            The Connection
     * @param ipAddress
     *            The ipAddress of the client
     * @param sender
     *            The mailFrom
     * @param recip
     *            The rcptTo
     * @return data
     *            The data
     * @throws SQLException
     */
    private Iterator<String> getGreyListData(Connection conn, String ipAddress,
        String sender, String recip) throws SQLException {
        Collection<String> data = new ArrayList<String>(2);
        PreparedStatement mappingStmt = null;
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
     * Insert new triplet in the store
     * 
     * @param conn
     *            The Connection
     * @param ipAddress
     *            The ipAddress of the client
     * @param sender
     *            The mailFrom
     * @param recip
     *            The rcptTo
     * @param count
     *            The count
     * @param createTime
     *            The createTime
     * @throws SQLException
     */
    private void insertTriplet(Connection conn, String ipAddress,
        String sender, String recip, int count, long createTime)
        throws SQLException {

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
     * Update the triplet
     * 
     * @param conn
     *            The Connection
     * 
     * @param ipAddress
     *            The ipAddress of the client
     * @param sender
     *            The mailFrom
     * @param recip
     *            The rcptTo
     * @param count
     *            The count
     * @param time
     *            the current time in ms
     * @throws SQLException
     */
    private void updateTriplet(Connection conn, String ipAddress,
        String sender, String recip, int count, long time)
        throws SQLException {

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
     * Cleanup the autowhitelist
     * 
     * @param conn
     *            The Connection
     * @param time
     *            The time which must be reached before delete the records
     * @throws SQLException
     */
    private void cleanupAutoWhiteListGreyList(Connection conn, long time)
        throws SQLException {
        PreparedStatement mappingStmt = null;

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
     * Cleanup the autowhitelist
     * 
     * @param conn
     *            The Connection
     * @param time
     *            The time which must be reached before delete the records
     * @throws SQLException
     */
    private void cleanupGreyList(Connection conn, long time)
        throws SQLException {
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
    public void initSqlQueries(Connection conn, String sqlFileUrl)
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
    private boolean createTable(Connection conn, String tableNameSqlStringName,
    String createSqlStringName) throws SQLException {
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
     * @see org.apache.james.smtpserver.protocol.hook.RcptHook#doRcpt(org.apache.james.smtpserver.protocol.SMTPSession, org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
     */
    public HookResult doRcpt(SMTPSession session, MailAddress sender, MailAddress rcpt) {
        if (!session.isRelayingAllowed()) {

            if ((wNetworks == null) || (!wNetworks.matchInetNetwork(session.getRemoteIPAddress()))) {
                return doGreyListCheck(session, sender,rcpt);
            } else {
                session.getLogger().info("IpAddress " + session.getRemoteIPAddress() + " is whitelisted. Skip greylisting.");
            }
        } else {
            session.getLogger().info("IpAddress " + session.getRemoteIPAddress() + " is allowed to send. Skip greylisting.");
        }
        return new HookResult(HookReturnCode.DECLINED);
    }
}
