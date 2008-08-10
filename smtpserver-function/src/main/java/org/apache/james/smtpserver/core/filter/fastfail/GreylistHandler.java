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

package org.apache.james.smtpserver.core.filter.fastfail;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.sql.Timestamp;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;

import org.apache.james.api.dnsserver.DNSServer;
import org.apache.james.services.FileSystem;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.JDBCUtil;
import org.apache.james.util.NetMatcher;
import org.apache.james.util.SqlResources;
import org.apache.james.util.TimeConverter;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.MailAddress;

/**
 * GreylistHandler which can be used to activate Greylisting
 */
public class GreylistHandler extends AbstractLogEnabled implements
    CommandHandler, Configurable, Serviceable, Initializable {

    private DataSourceSelector datasources = null;

    private DataSourceComponent datasource = null;

    private FileSystem fileSystem = null;

    // 1 hour
    private long tempBlockTime = 3600000;

    // 36 days
    private long autoWhiteListLifeTime = 3110400000L;

    // 4 hours
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
    private Map sqlParameters = new HashMap();

    /**
     * The repositoryPath
     */
    private String repositoryPath;

    private DNSServer dnsServer;

    private NetMatcher wNetworks;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration) throws ConfigurationException {
        Configuration configTemp = handlerConfiguration.getChild("tempBlockTime", false);
        if (configTemp != null) {
            try {
                setTempBlockTime(configTemp.getValue());

            } catch (NumberFormatException e) {
               throw new ConfigurationException(e.getMessage());
            }
        }
    
        Configuration configAutoWhiteList = handlerConfiguration.getChild("autoWhiteListLifeTime", false);
        if (configAutoWhiteList != null) {
            try {
                setAutoWhiteListLifeTime(configAutoWhiteList.getValue());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(e.getMessage());
            }
        }
       
        Configuration configUnseen = handlerConfiguration.getChild("unseenLifeTime", false);
        if (configUnseen != null) {
            try {
                setUnseenLifeTime(configUnseen.getValue());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(e.getMessage());
            }
        }

        Configuration configRepositoryPath = handlerConfiguration.getChild("repositoryPath", false);
        if (configRepositoryPath != null) {
            setRepositoryPath(configRepositoryPath.getValue());
        } else {
            throw new ConfigurationException("repositoryPath is not configured");
        }

        // Get the SQL file location
        Configuration sFile = handlerConfiguration.getChild("sqlFile", false);
        if (sFile != null) {
            setSqlFileUrl(sFile.getValue());
            if (!sqlFileUrl.startsWith("file://")) {
                throw new ConfigurationException(
                    "Malformed sqlFile - Must be of the format \"file://<filename>\".");
            }
        } else {
            throw new ConfigurationException("sqlFile is not configured");
        }

        Configuration whitelistedNetworks = handlerConfiguration.getChild("whitelistedNetworks", false);
        if (whitelistedNetworks != null) {
            Collection nets = whitelistedNetworks(whitelistedNetworks.getValue());

            if (nets != null) {
                wNetworks = new NetMatcher(nets,dnsServer);
                getLogger().info("Whitelisted addresses: " + wNetworks.toString());
            }
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        setDataSource(initDataSource(repositoryPath));
        initSqlQueries(datasource.getConnection(), sqlFileUrl);
        
        // create table if not exist
        createTable(datasource.getConnection(), "greyListTableName", "createGreyListTable");
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager serviceMan) throws ServiceException {
        setDataSources((DataSourceSelector) serviceMan.lookup(DataSourceSelector.ROLE));
        setDnsServer((DNSServer) serviceMan.lookup(DNSServer.ROLE));
        setFileSystem((FileSystem) serviceMan.lookup(FileSystem.ROLE));
    }

    /**
     * Set the DNSServer
     * 
     * @param dnsServer
     *            The DNSServer
     */
    public void setDnsServer(DNSServer dnsServer) {
        this.dnsServer = dnsServer;
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
     * Set the repositoryPath to use
     * 
     * @param repositoryPath
     *            The repositoryPath
     */
    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    /**
     * Set the datasources
     * 
     * @param datasources
     *            The datasources
     */
    public void setDataSources(DataSourceSelector datasources) {
        this.datasources = datasources;
    }

    /**
     * Sets the filesystem service
     * 
     * @param system The filesystem service
     */
    private void setFileSystem(FileSystem system) {
        this.fileSystem = system;
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

    /**
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        if (!session.isRelayingAllowed() && !(session.isAuthRequired() && session.getUser() != null)) {

            if ((wNetworks == null) || (!wNetworks.matchInetNetwork(session.getRemoteIPAddress()))) {
                doGreyListCheck(session, session.getCommandArgument());
            } else {
                getLogger().info("IpAddress " + session.getRemoteIPAddress() + " is whitelisted. Skip greylisting.");
            }
        } else {
            getLogger().info("IpAddress " + session.getRemoteIPAddress() + " is allowed to send. Skip greylisting.");
        }
    }

    /**
     * Handler method called upon receipt of a RCPT command. Calls a greylist
     * check
     * 
     * 
     * @param session
     *            SMTP session object
     * @param argument
     */
    private void doGreyListCheck(SMTPSession session, String argument) {
        String recip = "";
        String sender = "";
        MailAddress recipAddress = (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT);
        MailAddress senderAddress = (MailAddress) session.getState().get(SMTPSession.SENDER);

        if (recipAddress != null) recip = recipAddress.toString();
        if (senderAddress != null) sender = senderAddress.toString();
    
        long time = System.currentTimeMillis();
        String ipAddress = session.getRemoteIPAddress();
    
        try {
            long createTimeStamp = 0;
            int count = 0;
            
            // get the timestamp when he triplet was last seen
            Iterator data = getGreyListData(datasource.getConnection(), ipAddress, sender, recip);
            
            if (data.hasNext()) {
                createTimeStamp = Long.parseLong(data.next().toString());
                count = Integer.parseInt(data.next().toString());
            }
            
            getLogger().debug("Triplet " + ipAddress + " | " + sender + " | " + recip  +" -> TimeStamp: " + createTimeStamp);


            // if the timestamp is bigger as 0 we have allready a triplet stored
            if (createTimeStamp > 0) {
                long acceptTime = createTimeStamp + tempBlockTime;
        
                if ((time < acceptTime) && (count == 0)) {
                    String responseString = "451 " + DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.NETWORK_DIR_SERVER) 
                        + " Temporary rejected: Reconnect to fast. Please try again later";

                    // reconnect to fast block it again
                    session.writeResponse(responseString);
                    session.setStopHandlerProcessing(true);

                } else {
                    
                    getLogger().debug("Update triplet " + ipAddress + " | " + sender + " | " + recip + " -> timestamp: " + time);
                    
                    // update the triplet..
                    updateTriplet(datasource.getConnection(), ipAddress, sender, recip, count, time);

                }
            } else {
                getLogger().debug("New triplet " + ipAddress + " | " + sender + " | " + recip );
           
                // insert a new triplet
                insertTriplet(datasource.getConnection(), ipAddress, sender, recip, count, time);
      
                // Tempory block on new triplet!
                String responseString = "451 " + DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.NETWORK_DIR_SERVER) 
                    + " Temporary rejected: Please try again later";

                session.writeResponse(responseString);
                session.setStopHandlerProcessing(true);
            }

            // some kind of random cleanup process
            if (Math.random() > 0.99) {
                // cleanup old entries
            
                getLogger().debug("Delete old entries");
            
                cleanupAutoWhiteListGreyList(datasource.getConnection(),(time - autoWhiteListLifeTime));
                cleanupGreyList(datasource.getConnection(), (time - unseenLifeTime));
            }

        } catch (SQLException e) {
            // just log the exception
            getLogger().error("Error on SQLquery: " + e.getMessage());
        }
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
    private Iterator getGreyListData(Connection conn, String ipAddress,
        String sender, String recip) throws SQLException {
        Collection data = new ArrayList(2);
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
            getLogger().debug("JDBCVirtualUserTable: " + logString);
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

            InputStream sqlFile = null;
    
            try {
                sqlFile = fileSystem.getResource(sqlFileUrl);
                sqlFileUrl = null;
            } catch (Exception e) {
                getLogger().fatalError(e.getMessage(), e);
                throw e;
            }

            sqlQueries.init(sqlFile, "GreyList", conn, sqlParameters);

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

            StringBuffer logBuffer = null;
            logBuffer = new StringBuffer(64).append("Created table '").append(tableName)
            .append("' using sqlResources string '")
            .append(createSqlStringName).append("'.");
        getLogger().info(logBuffer.toString());

        } finally {
            theJDBCUtil.closeJDBCStatement(createStatement);
        }
        return true;
    }

    /**
     * Return a Collection which holds the values of the given string splitted
     * on ","
     * 
     * @param networks
     *            The commaseperated list of values
     * @return wNetworks The Collection which holds the whitelistNetworks
     */
    private Collection whitelistedNetworks(String networks) {
        Collection wNetworks = null;
        StringTokenizer st = new StringTokenizer(networks, ", ", false);
        wNetworks = new ArrayList();
        
        while (st.hasMoreTokens())
            wNetworks.add(st.nextToken());
        return wNetworks;
    }

    public Collection getImplCommands() {
        Collection c = new ArrayList();
        c.add("RCPT");
        return c;
    }
}
