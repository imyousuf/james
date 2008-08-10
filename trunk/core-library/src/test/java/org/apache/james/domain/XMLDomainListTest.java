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
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.api.dnsservice.DNSServer;
import org.apache.james.services.AbstractDNSServer;
import org.apache.james.services.ManageableDomainList;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;

import junit.framework.TestCase;

public class XMLDomainListTest extends TestCase {
    
    private Configuration setUpConfiguration(boolean auto,boolean autoIP,ArrayList names) {
        DefaultConfiguration configuration = new DefaultConfiguration("test");
        DefaultConfiguration sNamesConf = new DefaultConfiguration("domainnames");
        DefaultConfiguration autoConf = new DefaultConfiguration("autodetect");
        autoConf.setValue(auto);
        configuration.addChild(autoConf);
        
        DefaultConfiguration autoIPConf = new DefaultConfiguration("autodetectIP");
        autoIPConf.setValue(autoIP);
        configuration.addChild(autoIPConf);

        for (int i= 0; i< names.size(); i++) {
            DefaultConfiguration nameConf = new DefaultConfiguration("domainname");
            
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
    
    public void testGetDomains() throws Exception {
        ArrayList domains = new ArrayList();
        domains.add("domain1.");
        domains.add("domain2.");
    
        XMLDomainList dom = new XMLDomainList();
        ContainerUtil.enableLogging(dom,new MockLogger());
        ContainerUtil.service(dom,setUpServiceManager(setUpDNSServer("localhost")));
        ContainerUtil.configure(dom, setUpConfiguration(false,false,domains));
        ContainerUtil.initialize(dom);

        assertTrue("Two domain found",dom.getDomains().size() ==2);
    }
    
    public void testGetDomainsAutoDetectNotLocalHost() throws Exception {
        ArrayList domains = new ArrayList();
        domains.add("domain1.");
    
        XMLDomainList dom = new XMLDomainList();
        ContainerUtil.enableLogging(dom,new MockLogger());
        ContainerUtil.service(dom,setUpServiceManager(setUpDNSServer("local")));
        ContainerUtil.configure(dom, setUpConfiguration(true,false,domains));
        ContainerUtil.initialize(dom);
        
        assertEquals("Two domains found",dom.getDomains().size(), 2);
    }
    
    public void testGetDomainsAutoDetectLocalHost() throws Exception {
        ArrayList domains = new ArrayList();
        domains.add("domain1.");
    
        ManageableDomainList dom = new XMLDomainList();
        ContainerUtil.enableLogging(dom,new MockLogger());
        ContainerUtil.service(dom,setUpServiceManager(setUpDNSServer("localhost")));
        ContainerUtil.configure(dom, setUpConfiguration(true,false,domains));
        ContainerUtil.initialize(dom);
        
        assertEquals("One domain found",dom.getDomains().size(), 1);
    }
}
