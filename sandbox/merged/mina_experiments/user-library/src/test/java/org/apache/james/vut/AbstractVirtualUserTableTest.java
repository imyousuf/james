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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.dnsservice.TemporaryResolutionException;
import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.management.InvalidMappingException;
import org.apache.james.impl.vut.AbstractVirtualUserTable;

import junit.framework.TestCase;

public abstract class AbstractVirtualUserTableTest extends TestCase {

    protected AbstractVirtualUserTable virtualUserTable;
    protected final static int REGEX_TYPE = 0;
    protected final static int ERROR_TYPE = 1;
    protected final static int ADDRESS_TYPE = 2;
    protected final static int ALIASDOMAIN_TYPE = 3;
    
    protected void setUp() throws Exception {
        virtualUserTable = getVirtalUserTable();
    }
    
    protected void tearDown() throws Exception {
        Map mappings = virtualUserTable.getAllMappings();
        
        if (mappings != null) {
            Iterator mappingsIt = virtualUserTable.getAllMappings().keySet().iterator();
    
    
            while(mappingsIt.hasNext()) {
                String key = mappingsIt.next().toString();
                String args[] = key.split("@");
        
                Collection map = (Collection) mappings.get(key);
        
                Iterator mapIt = map.iterator();
        
                while (mapIt.hasNext()) {
                    try {
                        removeMapping(args[0], args[1], mapIt.next().toString());
                    } catch (InvalidMappingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        ContainerUtil.dispose(virtualUserTable);
    }
    
    private void removeMapping(String user, String domain, String rawMapping) throws InvalidMappingException {
        if (rawMapping.startsWith(VirtualUserTable.ERROR_PREFIX)) {
            removeMapping(user, domain, rawMapping.substring(VirtualUserTable.ERROR_PREFIX.length()), ERROR_TYPE);
        } else if (rawMapping.startsWith(VirtualUserTable.REGEX_PREFIX)) {
            removeMapping(user, domain, rawMapping.substring(VirtualUserTable.REGEX_PREFIX.length()), REGEX_TYPE);
        } else if (rawMapping.startsWith(VirtualUserTable.ALIASDOMAIN_PREFIX)) {
            removeMapping(user, domain, rawMapping.substring(VirtualUserTable.ALIASDOMAIN_PREFIX.length()), ALIASDOMAIN_TYPE);
        } else {
            removeMapping(user, domain, rawMapping, ADDRESS_TYPE);
        }
    }
    
    protected abstract AbstractVirtualUserTable getVirtalUserTable() throws ServiceException, ConfigurationException, Exception;
    
    protected abstract boolean addMapping(String user , String domain, String mapping,int type)throws InvalidMappingException;
    
    protected abstract boolean removeMapping(String user, String domain, String mapping, int type) throws InvalidMappingException;
    
 
    protected DNSService setUpDNSServer() {
        DNSService dns = new DNSService() {
            public String getHostName(InetAddress inet) {
                return "test";
            }
            
            public InetAddress[] getAllByName(String name) throws UnknownHostException {
                return new InetAddress[] { InetAddress.getByName("127.0.0.1")};        
            }
            
            public InetAddress getLocalHost() throws UnknownHostException {
                return InetAddress.getLocalHost();
            }

            public Collection findMXRecords(String hostname)
                    throws TemporaryResolutionException {
                throw new UnsupportedOperationException("Should never be called");
            }

            public Collection findTXTRecords(String hostname) {
                throw new UnsupportedOperationException("Should never be called");
            }

            public InetAddress getByName(String host)
                    throws UnknownHostException {
                throw new UnsupportedOperationException("Should never be called");
            }

            public Iterator getSMTPHostAddresses(String domainName)
                    throws TemporaryResolutionException {
                throw new UnsupportedOperationException("Should never be called");
            }
        };
        return dns;
    }
    
    public void testStoreAndRetrieveRegexMapping() throws ErrorMappingException {
        String user = "test";
        String domain = "localhost";
        String regex = "(.*):{$1}@localhost";
        String regex2 = "(.+):{$1}@test"; 
        String invalidRegex = ".*):";
        boolean catched = false;
        try {
        
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
        
            assertTrue("Added virtual mapping", addMapping(user, domain, regex, REGEX_TYPE));
            assertTrue("Added virtual mapping", addMapping(user, domain, regex2, REGEX_TYPE));

            assertEquals("Two mappings",virtualUserTable.getMappings(user, domain).size(), 2);           
            assertEquals("One mappingline",virtualUserTable.getAllMappings().size(),1);
           
            // Test DomainList implementations!
            assertEquals("Three domains",virtualUserTable.getDomains().size(), 3);
            assertTrue("Contains Domain",virtualUserTable.containsDomain(domain));
            
            assertTrue("remove virtual mapping", removeMapping(user, domain, regex, REGEX_TYPE));
        
            try {
                assertTrue("Added virtual mapping", virtualUserTable.addRegexMapping(user, domain, invalidRegex));
            } catch (InvalidMappingException e) {
                catched = true;
            }
            assertTrue("Invalid Mapping throw exception" , catched);

            assertTrue("remove virtual mapping", removeMapping(user, domain, regex2, REGEX_TYPE));
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
            
            assertNull("No mappings",virtualUserTable.getAllMappings());
            
        } catch (InvalidMappingException e) {
            fail("Storing failed");
        }
    
    }
    

    public void testStoreAndRetrieveAddressMapping() throws ErrorMappingException {
        String user = "test";
        String domain = "localhost";
        String address = "test@localhost2";
        String address2 = "test@james";
        
        try {
        
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
        
            assertTrue("Added virtual mapping", addMapping(user, domain, address, ADDRESS_TYPE));
            assertTrue("Added virtual mapping", addMapping(user, domain, address2, ADDRESS_TYPE));

            assertEquals("Two mappings",virtualUserTable.getMappings(user, domain).size(),2);
            assertEquals("One mappingline",virtualUserTable.getAllMappings().size(),1);
        
            assertTrue("remove virtual mapping", removeMapping(user, domain, address, ADDRESS_TYPE));
        
            /* TEMPORARILY REMOVE JDBC specific test 
            String invalidAddress= ".*@localhost2:";
            boolean catched = false;
            if (virtualUserTable instanceof JDBCVirtualUserTable) {
                try {
                    assertTrue("Added virtual mapping", addMapping(user, domain, invalidAddress, ADDRESS_TYPE));
                } catch (InvalidMappingException e) {
                    catched = true;
                } 
                assertTrue("Invalid Mapping throw exception" , catched);
            }         
            */
            
            assertTrue("remove virtual mapping", removeMapping(user, domain, address2, ADDRESS_TYPE));
            
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
            assertNull("No mappings",virtualUserTable.getAllMappings());
            
        } catch (InvalidMappingException e) {
            fail("Storing failed");
        }
        
    
    }

    public void testStoreAndRetrieveErrorMapping() throws ErrorMappingException { 
        String user = "test";
        String domain = "localhost";
        String error = "bounce!";
        boolean catched = false;
        
        try {
        
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
        
            assertTrue("Added virtual mapping", addMapping(user, domain, error, ERROR_TYPE));
            assertEquals("One mappingline",virtualUserTable.getAllMappings().size(),1);

            try {
                virtualUserTable.getMappings(user, domain);
            } catch (ErrorMappingException e) {
                catched = true;
            }
            assertTrue("Error Mapping throw exception" , catched);

            assertTrue("remove virtual mapping", removeMapping(user, domain, error, ERROR_TYPE));
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
            assertNull("No mappings",virtualUserTable.getAllMappings());
            
        } catch (InvalidMappingException e) {
             fail("Storing failed");
        }

    }
    
    public void testStoreAndRetrieveWildCardAddressMapping() throws ErrorMappingException {        
        String user = "test";
        String user2 = "test2";
        String domain = "localhost";
        String address = "test@localhost2";
        String address2 = "test@james";


       try {
                 
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
        
            assertTrue("Added virtual mapping", addMapping(null, domain, address, ADDRESS_TYPE));
            assertTrue("Added virtual mapping", addMapping(user, domain, address2, ADDRESS_TYPE));

          
            assertTrue("One mappings",virtualUserTable.getMappings(user, domain).size() == 1);
            assertTrue("One mappings",virtualUserTable.getMappings(user2, domain).size() == 1);
           
            assertTrue("remove virtual mapping", removeMapping(user, domain, address2, ADDRESS_TYPE));
            assertTrue("remove virtual mapping", removeMapping(null, domain, address, ADDRESS_TYPE));
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
            assertNull("No mapping",virtualUserTable.getMappings(user2, domain));
      
        } catch (InvalidMappingException e) {
           fail("Storing failed");
        }
    
    }
    
    public void testRecursiveMapping() throws ErrorMappingException {
        String user1 = "user1";
        String user2 = "user2";
        String user3 = "user3";
        String domain1 = "domain1";
        String domain2 = "domain2";
        String domain3 = "domain3";
        boolean exception1 = false;
    
        virtualUserTable.setRecursiveMapping(true);
       
        try {
            assertNull("No mappings",virtualUserTable.getAllMappings());
     
            assertTrue("Add mapping", addMapping(user1, domain1, user2 + "@" + domain2, ADDRESS_TYPE));
            assertTrue("Add mapping", addMapping(user2, domain2, user3 + "@" + domain3, ADDRESS_TYPE));
            assertEquals("Recursive mapped", virtualUserTable.getMappings(user1, domain1).iterator().next(),user3 + "@" + domain3);

            assertTrue("Add mapping", addMapping(user3, domain3, user1 + "@" + domain1, ADDRESS_TYPE));
            try {
                virtualUserTable.getMappings(user1, domain1);
            } catch (ErrorMappingException e) {
                exception1 = true;
            }
            assertTrue("Exception thrown on to many mappings", exception1);
            
            // disable recursive mapping
            virtualUserTable.setRecursiveMapping(false);
            assertEquals("Not recursive mapped", virtualUserTable.getMappings(user1, domain1).iterator().next(),user2 + "@" + domain2);
            
        } catch (InvalidMappingException e) {
            fail("Storing failed");
        }
    }
   
    
    public void testAliasDomainMapping() throws ErrorMappingException {
        String domain = "realdomain";
        String aliasDomain = "aliasdomain";
        String user = "user";
        String user2 = "user2";
    
        assertNull("No mappings",virtualUserTable.getAllMappings());
        try {
            assertTrue("Add mapping",addMapping(null, aliasDomain, user2 + "@" + domain, ADDRESS_TYPE));
            assertTrue("Add aliasDomain mapping", addMapping(null, aliasDomain, domain, ALIASDOMAIN_TYPE));
        
            Iterator mappings = virtualUserTable.getMappings(user, aliasDomain).iterator();
            assertEquals("Domain mapped as first ", mappings.next(), user + "@" + domain);
            assertEquals("Address mapped as second ", mappings.next(), user2 + "@" + domain);
            
            assertTrue("Remove mapping", removeMapping(null, aliasDomain, user2 + "@" + domain, ADDRESS_TYPE));
            assertTrue("Remove aliasDomain mapping", removeMapping(null, aliasDomain, domain, ALIASDOMAIN_TYPE));
        
        } catch (InvalidMappingException e) {
            fail("Storing failed");
        }
    }
    
}
