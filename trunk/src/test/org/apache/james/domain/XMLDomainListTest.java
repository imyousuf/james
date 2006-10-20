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
import java.util.ArrayList;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.services.AbstractDNSServer;
import org.apache.james.services.DNSServer;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;

import junit.framework.TestCase;

public class XMLDomainListTest extends TestCase {
    
    private Configuration setUpConfiguration(boolean auto,boolean autoIP,ArrayList names) {
        DefaultConfiguration configuration = new DefaultConfiguration("test");
        DefaultConfiguration sNamesConf = new DefaultConfiguration("servernames");
        sNamesConf.setAttribute("autodetect", auto);
        sNamesConf.setAttribute("autodetectIP", autoIP);
        
        for (int i= 0; i< names.size(); i++) {
            DefaultConfiguration nameConf = new DefaultConfiguration("servername");
            
            nameConf.setValue(names.get(i).toString());
            sNamesConf.addChild(nameConf);
        }

        configuration.addChild(sNamesConf);
        return configuration;
    }
    
    private DNSServer setUpDNSServer(final String hostName) {
        DNSServer dns = new AbstractDNSServer() {
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
    
    private MockServiceManager setUpServiceManager(DNSServer dns) {
        MockServiceManager service = new MockServiceManager();
        service.put(DNSServer.ROLE, dns);
        return service;
    }
    
    public void testGetDomains() throws ConfigurationException, ServiceException {
        ArrayList domains = new ArrayList();
        domains.add("domain1.");
    
        XMLDomainList dom = new XMLDomainList();
        ContainerUtil.enableLogging(dom,new MockLogger());
        dom.service(setUpServiceManager(setUpDNSServer("localhost")));
        dom.configure(setUpConfiguration(false,false,domains));

        assertTrue("One domain found",dom.getDomains().size() ==1);
    }
    
    public void testGetDomainsAutoDetectNotLocalHost() throws ConfigurationException, ServiceException {
        ArrayList domains = new ArrayList();
        domains.add("domain1.");
    
        XMLDomainList dom = new XMLDomainList();
        ContainerUtil.enableLogging(dom,new MockLogger());
        dom.service(setUpServiceManager(setUpDNSServer("hostname")));
        dom.configure(setUpConfiguration(true,false,domains));

        assertTrue("One domain found",dom.getDomains().size() == 2);
    }
    
    public void testGetDomainsAutoDetectLocalHost() throws ConfigurationException, ServiceException {
        ArrayList domains = new ArrayList();
        domains.add("domain1.");
    
        XMLDomainList dom = new XMLDomainList();
        ContainerUtil.enableLogging(dom,new MockLogger());
        dom.service(setUpServiceManager(setUpDNSServer("localhost")));
        dom.configure(setUpConfiguration(true,false,domains));

        assertTrue("One domain found",dom.getDomains().size() == 1);
    }

    public void testThrowConfigurationException() throws ConfigurationException, ServiceException {
        boolean exception = false;
        boolean exception2 = false;
        XMLDomainList dom = new XMLDomainList();
        ContainerUtil.enableLogging(dom,new MockLogger());
        dom.service(setUpServiceManager(setUpDNSServer("localhost")));
        try {
            dom.configure(new DefaultConfiguration("invalid"));
        } catch (ConfigurationException e) {
            exception = true;
        }
    
        assertTrue("Exception thrown",exception);
    
        try {
            dom.configure(setUpConfiguration(true,false,new ArrayList()));
        } catch (ConfigurationException e) {
            exception2 = true;
        }
    
        assertTrue("Exception thrown",exception2);
    }
}
