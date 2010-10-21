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
package org.apache.james.domainlist;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import junit.framework.TestCase;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.dnsservice.api.MockDNSService;
import org.apache.james.domainlist.JPADomainList;
import org.apache.james.domainlist.model.JPADomain;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;

/**
 * Test the JPA implementation of the DomainList.
 */
public class JPADomainListTest extends TestCase {
    
    // Domains we will play with.
    private final String DOMAIN_1 = "domain1.tld";
    private final String DOMAIN_2 = "domain2.tld";
    private final String DOMAIN_3 = "domain3.tld";
    private final String DOMAIN_4 = "domain4.tld";
    private final String DOMAIN_5 = "domain5.tld";

    /**
     * The OpenJPA Entity Manager used for the tests.
     */
    private OpenJPAEntityManagerFactory factory;

    /**
     * The properties for the OpenJPA Entity Manager.
     */
    private HashMap<String, String> properties;
    
    /**
     * The JPA DomainList service.
     */
    private JPADomainList jpaDomainList;
    
    @Override
    protected void setUp() throws Exception {

        super.setUp();
        
        // Use a memory database.
        properties = new HashMap<String, String>();
        properties.put("openjpa.ConnectionDriverName", org.apache.derby.jdbc.EmbeddedDriver.class.getName());
        properties.put("openjpa.ConnectionURL", "jdbc:derby:memory:JPADomainListTestDB;create=true");
        properties.put("openjpa.Log", "JDBC=WARN, SQL=WARN, Runtime=WARN");
        properties.put("openjpa.ConnectionFactoryProperties", "PrettyPrint=true, PrettyPrintLineLength=72");
        properties.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
        properties.put("openjpa.MetaDataFactory", "jpa(Types=" + JPADomain.class.getName() +")");
        factory = OpenJPAPersistence.getEntityManagerFactory(properties);
        
        // Initialize the JPADomainList (no autodetect,...).
        jpaDomainList = new JPADomainList();
        jpaDomainList.setLog(new SimpleLog("JPADomainListMockLog"));
        jpaDomainList.setDNSService(setUpDNSServer("localhost"));
        jpaDomainList.setAutoDetect(false);
        jpaDomainList.setAutoDetectIP(false);
        jpaDomainList.setEntityManagerFactory(factory);

        // Always delete everything before running any tests.
        deleteAll();
    
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Add 3 domains and list them.
     */
    public void createListDomains() {
        assertEquals(true, jpaDomainList.addDomain(DOMAIN_3));
        assertEquals(true, jpaDomainList.addDomain(DOMAIN_4));
        assertEquals(true, jpaDomainList.addDomain(DOMAIN_5));
        assertEquals(3, jpaDomainList.getDomains().length);
    }

    /**
     * Add a domain and check it is present.
     */
    public void testAddContainsDomain() {
        assertEquals(true, jpaDomainList.addDomain(DOMAIN_2));
        assertEquals(true, jpaDomainList.containsDomain(DOMAIN_2));
    }

    /**
     * Add and remove a domain, and check database is empty.
     */
    public void testAddRemoveContainsSameDomain() {
        assertEquals(true, jpaDomainList.addDomain(DOMAIN_1));
        assertEquals(true, jpaDomainList.removeDomain(DOMAIN_1));
        assertEquals(null, jpaDomainList.getDomains());
    }

    /**
     * Add a domain and remove another domain, and check first domain is still present.
     */
    public void testAddRemoveContainsDifferentDomain() {
        assertEquals(true, jpaDomainList.addDomain(DOMAIN_1));
        assertEquals(true, jpaDomainList.removeDomain(DOMAIN_2));
        assertEquals(1, jpaDomainList.getDomains().length);
        assertEquals(true, jpaDomainList.containsDomain(DOMAIN_1));
    }
    
    /**
     * Delete all possible domains from database.
     */
    private void deleteAll() {
        assertEquals(true, jpaDomainList.removeDomain(DOMAIN_1));
        assertEquals(true, jpaDomainList.removeDomain(DOMAIN_2));
        assertEquals(true, jpaDomainList.removeDomain(DOMAIN_3));
        assertEquals(true, jpaDomainList.removeDomain(DOMAIN_4));
        assertEquals(true, jpaDomainList.removeDomain(DOMAIN_5));
    }

    /**
     * Return a fake DNSServer.
     * 
     * @param hostName
     * @return
     */
    private DNSService setUpDNSServer(final String hostName) {
        DNSService dns = new MockDNSService() {
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
   
}
