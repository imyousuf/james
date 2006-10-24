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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceException;

import org.apache.james.services.AbstractDNSServer;
import org.apache.james.services.DNSServer;
import org.apache.james.services.FileSystem;

import org.apache.james.test.mock.avalon.MockLogger;

import org.apache.james.test.mock.james.MockFileSystem;
import org.apache.james.test.mock.util.AttrValConfiguration;
import org.apache.james.test.util.Util;

public class JDBCVirtualUserTableTest extends AbstractVirtualUserTableTest {
    
    protected AbstractVirtualUserTable getVirtalUserTable() throws ServiceException, ConfigurationException, Exception {
        DefaultServiceManager serviceManager = new DefaultServiceManager();
        serviceManager.put(FileSystem.ROLE, new MockFileSystem());
        serviceManager.put(DataSourceSelector.ROLE, Util.getDataSourceSelector());
        serviceManager.put(DNSServer.ROLE, setUpDNSServer());
        JDBCVirtualUserTable mr = new JDBCVirtualUserTable();
        

        mr.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration = new DefaultConfiguration("ReposConf");
        defaultConfiguration.setAttribute("destinationURL","db://maildb/VirtualUserTable");
        defaultConfiguration.addChild(new AttrValConfiguration("sqlFile","file://conf/sqlResources.xml"));
        mr.service(serviceManager);
        mr.configure(defaultConfiguration);
        mr.initialize();
        return mr;
    }
    
    private DNSServer setUpDNSServer() {
        DNSServer dns = new AbstractDNSServer() {
            public String getHostName(InetAddress inet) {
                return "test";
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
    
    public void testStoreAndRetrieveWildCardAddressMapping() throws ErrorMappingException {
        
        String user = "test";
        String user2 = "test2";
        String domain = "localhost";
        String address = "test@localhost2";
        String address2 = "test@james";


       try {
                 
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
        
            assertTrue("Added virtual mapping", virtualUserTable.addAddressMapping(null, domain, address));
            assertTrue("Added virtual mapping", virtualUserTable.addAddressMapping(user, domain, address2));

          
            assertTrue("One mappings",virtualUserTable.getMappings(user, domain).size() == 1);
            assertTrue("One mappings",virtualUserTable.getMappings(user2, domain).size() == 1);
           
            assertTrue("remove virtual mapping", virtualUserTable.removeAddressMapping(user, domain, address2));
            assertTrue("remove virtual mapping", virtualUserTable.removeAddressMapping(null, domain, address));
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
            assertNull("No mapping",virtualUserTable.getMappings(user2, domain));
      
        } catch (InvalidMappingException e) {
           fail("Storing failed");
        }
    
    }
    
}
