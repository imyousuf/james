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
package org.apache.james.vut;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.api.vut.management.VirtualUserTableManagementException;
import org.apache.james.impl.vut.AbstractVirtualUserTable;
import org.apache.james.impl.vut.VirtualUserTableUtil;
import org.apache.james.services.DataSourceSelector;
import org.apache.james.services.FileSystem;
import org.apache.james.util.sql.JDBCUtil;
import org.apache.james.util.sql.SqlResources;

/**
 * Class responsible to implement the Virtual User Table in database with JDBC access.
 * 
 * @deprecated use JPAVirtualUserTable
 */
@Deprecated
public class JDBCVirtualUserTable extends AbstractVirtualUserTable {

    private DataSourceSelector datasources = null;
    private DataSource dataSourceComponent = null;
    private String dataSourceName = null;
    
    private String tableName = "VirtualUserTable";

    /**
     * Contains all of the sql strings for this component.
     */
    private SqlResources sqlQueries;
    
    /**
     * The name of the SQL configuration file to be used to configure this repository.
     */
    private String sqlFileName;
    
    private FileSystem fileSystem;

    /**
     * The JDBCUtil helper class
     */
    private final JDBCUtil theJDBCUtil = new JDBCUtil() {
        protected void delegatedLog(String logString) {
            getLogger().debug("JDBCVirtualUserTable: " + logString);
        }
    };

    @PostConstruct
    public void init() throws Exception {
        
        setDataSource(datasources.getDataSource(dataSourceName));
    
        StringBuffer logBuffer = null;
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(this.getClass().getName() + ".initialize()");
        }

        // Test the connection to the database, by getting the DatabaseMetaData.
        Connection conn = dataSourceComponent.getConnection();
        PreparedStatement createStatement = null;

        try {
            // Initialise the sql strings.

            InputStream sqlFile = null;
            
            try {
                sqlFile = fileSystem.getResource(sqlFileName);
            } catch (Exception e) {
                getLogger().error(e.getMessage(), e);
                throw e;
            }

            if (getLogger().isDebugEnabled()) {
                logBuffer =
                    new StringBuffer(128)
                            .append("Reading SQL resources from file: ")
                            .append(sqlFileName)
                            .append(", section ")
                            .append(this.getClass().getName())
                            .append(".");
                getLogger().debug(logBuffer.toString());
            }

            // Build the statement parameters
            Map<String,String> sqlParameters = new HashMap<String,String>();
            if (tableName != null) {
                sqlParameters.put("table", tableName);
            }
            
            sqlQueries = new SqlResources();
            sqlQueries.init(sqlFile, this.getClass().getName(),
                            conn, sqlParameters);

            // Check if the required table exists. If not, create it.
            DatabaseMetaData dbMetaData = conn.getMetaData();
            
            // Need to ask in the case that identifiers are stored, ask the DatabaseMetaInfo.
            // Try UPPER, lower, and MixedCase, to see if the table is there.
            if (!(theJDBCUtil.tableExists(dbMetaData, tableName))) {
            
                // Users table doesn't exist - create it.
                createStatement =
                    conn.prepareStatement(sqlQueries.getSqlString("createTable", true));
                createStatement.execute();

                if (getLogger().isInfoEnabled()) {
                    logBuffer =
                        new StringBuffer(64)
                                .append("JdbcVirtalUserTable: Created table '")
                                .append(tableName)
                                .append("'.");
                    getLogger().info(logBuffer.toString());
                }
            }
            
   
        } finally {
            theJDBCUtil.closeJDBCStatement(createStatement);
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }
    
    @Resource(name="filesystem")
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }
    
    public void setDataSource(DataSource dataSourceComponent) {
        this.dataSourceComponent = dataSourceComponent;
    }
    
    @Resource(name="database-connections")
    public void setDataSourceSelector(DataSourceSelector datasources) {
        this.datasources = datasources;
    }

    protected void doConfigure(HierarchicalConfiguration conf) throws ConfigurationException {
        
        String destination = conf.getString("[@destinationURL]",null);
    
        if (destination == null) {
            throw new ConfigurationException("destinationURL must configured");
        }
    
        // normalize the destination, to simplify processing.
        if ( ! destination.endsWith("/") ) {
            destination += "/";
        }
        // Parse the DestinationURL for the name of the datasource,
        // the table to use, and the (optional) repository Key.
        // Split on "/", starting after "db://"
        List<String> urlParams = new ArrayList<String>();
        int start = 5;
        
        int end = destination.indexOf('/', start);
        while ( end > -1 ) {
            urlParams.add(destination.substring(start, end));
            start = end + 1;
            end = destination.indexOf('/', start);
        }
    
        // Build SqlParameters and get datasource name from URL parameters
        if (urlParams.size() == 0) {
            StringBuffer exceptionBuffer =
                new StringBuffer(256)
                        .append("Malformed destinationURL - Must be of the format '")
                        .append("db://<data-source>'.  Was passed ")
                        .append(conf.getString("[@destinationURL]"));
            throw new ConfigurationException(exceptionBuffer.toString());
        }
        
        if (urlParams.size() >= 1) {
            dataSourceName = (String)urlParams.get(0);
        }
        
        if (urlParams.size() >= 2) {
            tableName = (String)urlParams.get(1);
        }
    
        if (getLogger().isDebugEnabled()) {
            StringBuffer logBuffer =
                new StringBuffer(128)
                        .append("Parsed URL: table = '")
                        .append(tableName)
                        .append("'");
            getLogger().debug(logBuffer.toString());
        }
    
        sqlFileName = conf.getString("sqlFile");
        
    }

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#addMappingInternal(String, String, String)
     */
    protected boolean addMappingInternal(String user, String domain, String regex) throws VirtualUserTableManagementException {

        String newUser = getUserString(user);
        String newDomain = getDomainString(domain);
        Collection<String> map =  getUserDomainMappings(newUser,newDomain);

        if (map != null && map.size() != 0) {
            map.add(regex);
            return updateMapping(newUser,newDomain,VirtualUserTableUtil.CollectionToMapping(map));
        }
    
        return addRawMapping(newUser,newDomain,regex);
    
    }
    
    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#mapAddressInternal(java.lang.String, java.lang.String)
     */
    protected String mapAddressInternal(String user, String domain) {
        Connection conn = null;
        PreparedStatement mappingStmt = null;
        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(sqlQueries.getSqlString("selectMappings", true));

                ResultSet mappingRS = null;
                try {
                    mappingStmt.setString(1, user);
                    mappingStmt.setString(2, domain);
                    mappingRS = mappingStmt.executeQuery();
                    if (mappingRS.next()) {
                        return mappingRS.getString(1);
                    }
                } finally {
                    theJDBCUtil.closeJDBCResultSet(mappingRS);
                }
            
        } catch (SQLException sqle) {
            getLogger().error("Error accessing database", sqle);
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
        return null;
    }
    
    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#mapAddress(java.lang.String, java.lang.String)
     */
    protected Collection<String> getUserDomainMappingsInternal(String user, String domain) {
        Connection conn = null;
        PreparedStatement mappingStmt = null;
        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(sqlQueries.getSqlString("selectUserDomainMapping", true));
            ResultSet mappingRS = null;
            try {
                mappingStmt.setString(1, user);
                mappingStmt.setString(2, domain);
                mappingRS = mappingStmt.executeQuery();
                if (mappingRS.next()) {
                    return VirtualUserTableUtil.mappingToCollection(mappingRS.getString(1));
                }
            } finally {
                theJDBCUtil.closeJDBCResultSet(mappingRS);
            }
        } catch (SQLException sqle) {
            getLogger().error("Error accessing database", sqle);
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
        return null;
    }

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#getAllMappingsInternal()
     */
    protected Map<String,Collection<String>> getAllMappingsInternal() {
        Connection conn = null;
        PreparedStatement mappingStmt = null;
        Map<String,Collection<String>> mapping = new HashMap<String,Collection<String>>();
        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(sqlQueries.getSqlString("selectAllMappings", true));
            ResultSet mappingRS = null;
            try {
                mappingRS = mappingStmt.executeQuery();
                while(mappingRS.next()) {
                    String user = mappingRS.getString(1);
                    String domain = mappingRS.getString(2);
                    String map = mappingRS.getString(3);
                    mapping.put(user + "@" + domain, VirtualUserTableUtil.mappingToCollection(map));
                }
                if (mapping.size() > 0 ) return mapping;
            } finally {
                theJDBCUtil.closeJDBCResultSet(mappingRS);
            }
            
        } catch (SQLException sqle) {
            getLogger().error("Error accessing database", sqle);
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
        return null;
    }
    
    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#removeMappingInternal(String, String, String)
     */
    protected boolean removeMappingInternal(String user, String domain, String mapping) throws VirtualUserTableManagementException {
        String newUser = getUserString(user);
        String newDomain = getDomainString(domain);
        Collection<String> map = getUserDomainMappings(newUser,newDomain);
        if (map != null && map.size() > 1) {
            map.remove(mapping);
            return updateMapping(newUser,newDomain,VirtualUserTableUtil.CollectionToMapping(map));
        } else {
            return removeRawMapping(newUser,newDomain,mapping);
        }
    }

   /**
     * Update the mapping for the given user and domain
     * 
     * @param user the user
     * @param domain the domain
     * @param mapping the mapping
     * @return true if update was successfully
     */
    private boolean updateMapping(String user, String domain, String mapping) {
        Connection conn = null;
        PreparedStatement mappingStmt = null;

        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(sqlQueries.getSqlString(
                "updateMapping", true));

            ResultSet mappingRS = null;
            try {
                mappingStmt.setString(1, mapping);
                mappingStmt.setString(2, user);
                mappingStmt.setString(3, domain);
               
                if (mappingStmt.executeUpdate()> 0) {
                   return true;
                }
            } finally {
                theJDBCUtil.closeJDBCResultSet(mappingRS);
            }

        } catch (SQLException sqle) {
            getLogger().error("Error accessing database", sqle);
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
        return false;
    }
    
    
    /**
     * Remove a mapping for the given user and domain
     * 
     * @param user the user
     * @param domain the domain
     * @param mapping the mapping
     * @return true if succesfully
     */
    private boolean removeRawMapping(String user, String domain, String mapping) {
        Connection conn = null;
        PreparedStatement mappingStmt = null;

        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(sqlQueries.getSqlString(
            "deleteMapping", true));

            ResultSet mappingRS = null;
            try {
                mappingStmt.setString(1, user);
                mappingStmt.setString(2, domain);
                mappingStmt.setString(3, mapping);
                if(mappingStmt.executeUpdate() > 0) {
                    return true;
                }
            } finally {
               theJDBCUtil.closeJDBCResultSet(mappingRS);
            }

        } catch (SQLException sqle) {
            getLogger().error("Error accessing database", sqle);
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
        return false;
    }
    
    /**
     * Add mapping for given user and domain
     * 
     * @param user the user
     * @param domain the domain
     * @param mapping the mapping 
     * @return true if successfully
     */
    private boolean addRawMapping(String user, String domain, String mapping) {
        Connection conn = null;
        PreparedStatement mappingStmt = null;

        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(sqlQueries.getSqlString(
            "addMapping", true));

            ResultSet mappingRS = null;
            try {
                mappingStmt.setString(1, user);
                mappingStmt.setString(2, domain);
                mappingStmt.setString(3, mapping);
               
                if(mappingStmt.executeUpdate() >0) {
                    return true;
                }
            } finally {
                theJDBCUtil.closeJDBCResultSet(mappingRS);
            }

        } catch (SQLException sqle) {
            getLogger().error("Error accessing database", sqle);
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
        return false;
    }

    
    /**
     * Return user String for the given argument
     * 
     * @param user the given user String
     * @return user the user String
     * @throws InvalidMappingException get thrown on invalid argument
     */
    private String getUserString(String user) throws VirtualUserTableManagementException {
        if (user != null) {
            if(user.equals(WILDCARD) || user.indexOf("@") < 0) {
                return user;
            } else {
                throw new VirtualUserTableManagementException("Invalid user: " + user);
            }
        } else {
            return WILDCARD;
        }
    }
    
    /**
     * Return domain String for the given argument
     * 
     * @param domain the given domain String
     * @return domainString the domain String
     * @throws InvalidMappingException get thrown on invalid argument
     */
    private String getDomainString(String domain) throws VirtualUserTableManagementException {
        if(domain != null) {
            if (domain.equals(WILDCARD) || domain.indexOf("@") < 0) {
                return domain;  
            } else {
                throw new VirtualUserTableManagementException("Invalid domain: " + domain);
            }
        } else {
            return WILDCARD;
        }
    }
    
}
