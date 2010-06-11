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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.TestCase;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.excalibur.datasource.DataSourceComponent;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.services.FileSystem;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.james.MockFileSystem;
import org.apache.james.test.util.Util;
import org.apache.james.util.sql.JDBCUtil;

public class JDBCDomainListTest  extends TestCase {
    private String repos = "db://maildb/";
    private String table = "costumTable";
    private DataSourceSelector dataSource;
    private DataSourceComponent data;
    
    public void setUp() throws Exception {
        dataSource = Util.getDataSourceSelector();
        data = (DataSourceComponent) dataSource.select("maildb");
    
        sqlQuery("create table " + table + " (domain VARCHAR (255))");
    }
    
    public void tearDown() throws Exception {
        sqlQuery("drop table " + table);
    }

    private boolean sqlQuery(String query){
        Connection conn = null;
        PreparedStatement mappingStmt = null;

        try {
            conn = data.getConnection();
            mappingStmt = conn.prepareStatement(query);

            ResultSet mappingRS = null;
            try {
             
                if(mappingStmt.executeUpdate() >0) {
                    return true;
                }
            } finally {
                theJDBCUtil.closeJDBCResultSet(mappingRS);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            theJDBCUtil.closeJDBCStatement(mappingStmt);
            theJDBCUtil.closeJDBCConnection(conn);
        }
        return false;
    }
    
    /**
     * The JDBCUtil helper class
     */
    private final JDBCUtil theJDBCUtil = new JDBCUtil() {
        protected void delegatedLog(String logString) {}
    };
    
    private Configuration setUpConfiguration(String url) {
        DefaultConfiguration configuration = new DefaultConfiguration("test");
        DefaultConfiguration reposConf = new DefaultConfiguration("repositoryPath");          
        reposConf.setValue(url);
        configuration.addChild(reposConf);
        
        DefaultConfiguration sqlConf = new DefaultConfiguration("sqlFile");          
        sqlConf.setValue("file://conf/sqlResources.xml");
        configuration.addChild(sqlConf);

        return configuration;
    }
    
    private DNSService setUpDNSServer(final String hostName) {
        DNSService dns = new AbstractDNSServer() {
            public String getHostName(InetAddress inet) {
                return hostName;
            }
            
            public InetAddress[] getAllByName(String name) throws UnknownHostException {
                return new InetAddress[] { InetAddress.getByName("127.0.0.1")}; 
            }
            
            public InetAddress getLocalHost() throws UnknownHostException {
            return InetAddress.getLocalHost();
            }
        };
        return dns;
    }
    
    private MockServiceManager setUpServiceManager(DNSService dns) throws Exception {
        MockServiceManager service = new MockServiceManager();
        service.put(DNSService.ROLE, dns);
        service.put(FileSystem.ROLE, new MockFileSystem());
        service.put(DataSourceSelector.ROLE, dataSource);
        return service;
    }
    
    public void testAddRemoveGetDomains() throws Exception {
        
    
        JDBCDomainList dom = new JDBCDomainList();
        ContainerUtil.enableLogging(dom,new MockLogger());
        dom.service(setUpServiceManager(setUpDNSServer("localhost")));
        dom.configure(setUpConfiguration(repos + table));
        dom.initialize();
        dom.addDomain("domain1.");

        assertEquals("two domain found",dom.getDomains().size(),2);
        
        dom.removeDomain("domain1.");
        assertNull("two domain found",dom.getDomains());
        
    }
  

    public void testThrowConfigurationException() throws Exception {
        boolean exception = false;
        boolean exception2 = false;
        JDBCDomainList dom = new JDBCDomainList();
        ContainerUtil.enableLogging(dom,new MockLogger());
        dom.service(setUpServiceManager(setUpDNSServer("localhost")));
        try {
            dom.configure(new DefaultConfiguration("invalid"));
            dom.initialize();
        } catch (ConfigurationException e) {
            exception = true;
        }
    
        assertTrue("Exception thrown",exception);
    
        try {
            dom.configure(setUpConfiguration(null));
        } catch (ConfigurationException e) {
            exception2 = true;
        }
    
        assertTrue("Exception thrown",exception2);
    }
}
