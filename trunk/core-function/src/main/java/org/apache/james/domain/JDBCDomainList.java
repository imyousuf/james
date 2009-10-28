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



package org.apache.james.domain;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.services.FileSystem;
import org.apache.james.util.sql.JDBCUtil;
import org.apache.james.util.sql.SqlResources;

/**
 * Allow to query a costum table for domains
 */
public class JDBCDomainList extends AbstractDomainList {

    private DataSourceSelector datasources;
    private DataSourceComponent dataSourceComponent;
    private FileSystem fileSystem;
    
    private String tableName = null;
    private String dataSourceName = null;
    
    /**
     * Contains all of the sql strings for this component.
     */
    protected SqlResources sqlQueries;
    
    /**
     * The name of the SQL configuration file to be used to configure this repository.
     */
    private String sqlFileName;

    protected String datasourceName;
    
    private HierarchicalConfiguration configuration;

    @Resource(name="org.apache.commons.configuration.Configuration")
    public void setConfiguration(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setDataSourceComponent(DataSourceComponent dataSourceComponent) {
        this.dataSourceComponent = dataSourceComponent;
    }
    

    @Resource(name="org.apache.avalon.cornerstone.services.datasources.DataSourceSelector")
    public void setDataSourceSelector(DataSourceSelector datasources) {
        this.datasources = datasources;
    }
    
    @Resource(name="org.apache.james.services.FileSystem")
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }


    protected void configure() throws ConfigurationException {
        String destination = configuration.getString("repositoryPath",null);
        if (destination == null) {
            throw new ConfigurationException("RepositoryPath must configured");
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
        if (urlParams.size() != 2) {
            StringBuffer exceptionBuffer =
                new StringBuffer(256)
                        .append("Malformed destinationURL - Must be of the format '")
                        .append("db://<data-source>/<table>'.  Was passed ")
                        .append(configuration.getString("repositoryPath"));
            throw new ConfigurationException(exceptionBuffer.toString());
        }
        dataSourceName = (String)urlParams.get(0);
        tableName = (String)urlParams.get(1);


        if (getLogger().isDebugEnabled()) {
            StringBuffer logBuffer =
                new StringBuffer(128)
                        .append("Parsed URL: table = '")
                        .append(tableName)
                        .append("'");
            getLogger().debug(logBuffer.toString());
        }
    
        sqlFileName = configuration.getString("sqlFile",null);
        
        setAutoDetect(configuration.getBoolean("autodetect", true));    
        setAutoDetectIP(configuration.getBoolean("autodetectIP", true));    
        
    }
    
    @PostConstruct
    public void init() throws Exception {
        configure();
        
        setDataSourceComponent((DataSourceComponent) datasources.select(dataSourceName));
    
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
                sqlFileName = null;
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
    
    /**
     * The JDBCUtil helper class
     */
    private final JDBCUtil theJDBCUtil = new JDBCUtil() {
        protected void delegatedLog(String logString) {
            getLogger().debug("JDBCVirtualUserTable: " + logString);
        }
    };

    /**
     * @see org.apache.james.domain.AbstractDomainList#getDomainListInternal()
     */
    protected List<String> getDomainListInternal() {
        List<String> domains = new ArrayList<String>();
        Connection conn = null;
        PreparedStatement mappingStmt = null;
        
        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(sqlQueries.getSqlString("selectDomains", true));

            ResultSet mappingRS = null;
            try {
                mappingRS = mappingStmt.executeQuery();
                while (mappingRS.next()) {
                    String domain = mappingRS.getString(1).toLowerCase();
                    if(domains.contains(domains) == false) {
                        domains.add(domain);
                    }
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
        if (domains.size() == 0) {
            return null;
        } else {
            return domains;
        }
    }

    /**
     * @see org.apache.james.api.domainlist.DomainList#containsDomain(java.lang.String)
     */
    public boolean containsDomain(String domain) {
        Connection conn = null;
        PreparedStatement mappingStmt = null;
        
        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(sqlQueries.getSqlString("selectDomain", true));

            ResultSet mappingRS = null;
            try {
                mappingStmt.setString(1, domain);
                mappingRS = mappingStmt.executeQuery();
                if (mappingRS.next()) {
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
     * @see org.apache.james.domain.AbstractDomainList#addDomainInternal(java.lang.String)
     */
    protected boolean addDomainInternal(String domain) {
        Connection conn = null;
        PreparedStatement mappingStmt = null;
        
        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(sqlQueries.getSqlString("addDomain", true));

            ResultSet mappingRS = null;
            try {
            mappingStmt.setString(1, domain);
                if (mappingStmt.executeUpdate() > 0) {
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
     * @see org.apache.james.domain.AbstractDomainList#removeDomainInternal(java.lang.String)
     */
    protected boolean removeDomainInternal(String domain) {
        Connection conn = null;
        PreparedStatement mappingStmt = null;
        
        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(sqlQueries.getSqlString("removeDomain", true));

            ResultSet mappingRS = null;
            try {
            mappingStmt.setString(1, domain);
                if (mappingStmt.executeUpdate() > 0) {
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
}
