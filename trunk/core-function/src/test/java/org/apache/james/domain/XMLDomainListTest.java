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
import java.util.List;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.api.dnsservice.DNSService;

import junit.framework.TestCase;

public class XMLDomainListTest extends TestCase {
    
    private HierarchicalConfiguration setUpConfiguration(boolean auto,boolean autoIP, List<String> names) {
        DefaultConfigurationBuilder configuration = new DefaultConfigurationBuilder();

        configuration.addProperty("autodetect", auto);
        configuration.addProperty("autodetectIP", autoIP);
        for (int i= 0; i< names.size(); i++) {
            configuration.addProperty("domainnames.domainname", names.get(i).toString());
        }
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

    public void testGetDomains() throws Exception {
        List<String> domains = new ArrayList<String>();
        domains.add("domain1.");
        domains.add("domain2.");
    
        XMLDomainList dom = new XMLDomainList();
        dom.setLogger(new SimpleLog("MockLog"));
        dom.setDNSService(setUpDNSServer("localhost"));
        dom.setConfiguration(setUpConfiguration(false,false,domains));
        dom.init();

        assertTrue("Two domain found",dom.getDomains().size() ==2);
    }
    
    public void testGetDomainsAutoDetectNotLocalHost() throws Exception {
        List<String> domains = new ArrayList<String>();
        domains.add("domain1.");
    
        XMLDomainList dom = new XMLDomainList();
        dom.setLogger(new SimpleLog("MockLog"));
        dom.setDNSService(setUpDNSServer("local"));
        dom.setConfiguration(setUpConfiguration(true,false,domains));
        dom.init();
        assertEquals("Two domains found",dom.getDomains().size(), 2);
    }
    
    public void testGetDomainsAutoDetectLocalHost() throws Exception {
        List<String> domains = new ArrayList<String>();
        domains.add("domain1.");
    
        XMLDomainList dom = new XMLDomainList();
        dom.setLogger(new SimpleLog("MockLog"));
        dom.setDNSService(setUpDNSServer("localhost"));
        dom.setConfiguration(setUpConfiguration(true,false,domains));
        dom.init();
        
        assertEquals("One domain found",dom.getDomains().size(), 1);
    }
}
