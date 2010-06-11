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

import junit.framework.TestCase;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceException;

import org.apache.james.services.AbstractDNSServer;
import org.apache.james.services.DNSServer;
import org.apache.james.services.FileSystem;

import org.apache.james.test.mock.avalon.MockLogger;

import org.apache.james.test.mock.james.MockFileSystem;
import org.apache.james.test.mock.util.AttrValConfiguration;

import org.apache.james.test.util.Util;

public class XMLVirtualUserTableTest extends TestCase {
    private String user = "user1";
    private String domain = "anydomain";
    
    AbstractVirtualUserTable table;
    
    protected AbstractVirtualUserTable getVirtalUserTable() throws ServiceException, ConfigurationException, Exception {
        DefaultServiceManager serviceManager = new DefaultServiceManager();
        serviceManager.put(FileSystem.ROLE, new MockFileSystem());
        serviceManager.put(DataSourceSelector.ROLE, Util.getDataSourceSelector());
        serviceManager.put(DNSServer.ROLE, new AbstractDNSServer() {
            public InetAddress getLocalHost() throws UnknownHostException {
                return InetAddress.getLocalHost();
            }
            
            public InetAddress[] getAllByName(String domain) throws UnknownHostException{
                throw new UnknownHostException();
            }
            
            public String getHostName(InetAddress in) {
                return "localHost";      
            }
        });
        XMLVirtualUserTable mr = new XMLVirtualUserTable();
        

        mr.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration = new DefaultConfiguration("conf");
        defaultConfiguration.addChild(new AttrValConfiguration("mapping",user + "@" + domain +"=user2@localhost;user3@localhost"));
        defaultConfiguration.addChild(new AttrValConfiguration("mapping","*" + "@" + domain +"=user4@localhost;user5@localhost"));
        mr.configure(defaultConfiguration);
        mr.service(serviceManager);
        return mr;
    }
    
    public void setUp() throws Exception {
        table = getVirtalUserTable();
    }
    
    public void tearDown() {
        ContainerUtil.dispose(table);
    }
    
    public void testGetMappings() throws ErrorMappingException {
        assertEquals("Found 2 mappings", table.getMappings(user, domain).size(), 2);
        assertEquals("Found 2 domains", table.getDomains().size(),2);
    }
    
    public void testGetUserMappings() throws ErrorMappingException, InvalidMappingException {
        assertTrue("Found 1 mappings", table.getMappings("any", domain).size() == 2);
        assertNull("Found 0 mappings", table.getUserDomainMappings("any", domain));
    }
}
