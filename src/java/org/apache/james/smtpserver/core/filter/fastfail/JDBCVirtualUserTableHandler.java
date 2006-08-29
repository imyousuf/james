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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.util.JDBCUtil;
import org.apache.james.util.VirtualUserTableUtil;
import org.apache.mailet.MailAddress;

/**
 * 
 * Handler which check for valid mappings via JDBCVirtualUserTable
 */
public class JDBCVirtualUserTableHandler extends AbstractVirtualUserTableHandler implements Configurable,Serviceable,Initializable{
    
    private DataSourceSelector datasources = null;
    private DataSourceComponent dataSourceComponent = null;
    private String tableName = null;
    private String dataSourceName = null;
    private String query = VirtualUserTableUtil.QUERY; 

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration table = arg0.getChild("table",false);
        
        if (table != null) {
            String tableURL = table.getValue();

            String datasourceName = tableURL.substring(5);
            int pos = datasourceName.indexOf("/");
            tableName = datasourceName.substring(pos + 1);
            dataSourceName = datasourceName.substring(0, pos);
        } else {
            throw new ConfigurationException("Table location not specified for JDBCVirtualUserTableHandler");
        }
        
        Configuration queryConfig = arg0.getChild("query",false);
        
        if (queryConfig != null) {
            query = queryConfig.getValue();     
        } 
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        datasources = (DataSourceSelector)arg0.lookup(DataSourceSelector.ROLE);
    }
    

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        Connection conn = null;
    
        setDataSourceComponent((DataSourceComponent) datasources.select(dataSourceName));
    
        try {
        conn = dataSourceComponent.getConnection();
            if (!(theJDBCUtil.tableExists(conn.getMetaData(), tableName))) {
                StringBuffer exceptionBuffer =
                                              new StringBuffer(128)
                                              .append("Could not find table '")
                                              .append(tableName)
                                              .append("' in datasource '")
                                              .append(dataSourceName)
                                             .append("'");
                throw new Exception(exceptionBuffer.toString());
            }
        } finally {
            theJDBCUtil.closeJDBCConnection(conn);
        }
    }
    
    public void setDataSourceComponent(DataSourceComponent dataSourceComponent) {
        this.dataSourceComponent = dataSourceComponent;
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
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractVirtualUserTableHandler#mapRecipients(MailAddress)
     */
    protected String mapRecipients(MailAddress recipient) {
        Connection conn = null;
        PreparedStatement mappingStmt = null;
        try {
            conn = dataSourceComponent.getConnection();
            mappingStmt = conn.prepareStatement(query);

                ResultSet mappingRS = null;
                try {
                    mappingStmt.setString(1, recipient.getUser());
                    mappingStmt.setString(2, recipient.getHost());
                    mappingStmt.setString(3, recipient.getHost());
                    mappingRS = mappingStmt.executeQuery();
                    if (mappingRS.next()) {
                        String targetString = mappingRS.getString(1);
                        return targetString;
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

}
