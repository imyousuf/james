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

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.services.AbstractDNSServer;
import org.apache.james.services.DNSServer;

import junit.framework.TestCase;

public abstract class AbstractVirtualUserTableTest extends TestCase {

    protected AbstractVirtualUserTable virtualUserTable;
    protected final static int REGEX_TYPE = 0;
    protected final static int ERROR_TYPE = 1;
    protected final static int ADDRESS_TYPE = 2;
    
    protected void setUp() throws Exception {
        virtualUserTable = getVirtalUserTable();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        ContainerUtil.dispose(virtualUserTable);
    }
    
    protected abstract AbstractVirtualUserTable getVirtalUserTable() throws ServiceException, ConfigurationException, Exception;
    
    protected abstract boolean addMapping(String user , String domain, String mapping,int type)throws InvalidMappingException;
    
    protected abstract boolean removeMapping(String user, String domain, String mapping, int type) throws InvalidMappingException;
    
 
    protected DNSServer setUpDNSServer() {
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
            
            System.err.println("MAPPINGS:" + virtualUserTable.getAllMappings() + " domains:" + virtualUserTable.getDomains().size()); 
           
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
        String invalidAddress= ".*@localhost2:";
        boolean catched = false;
        
        try {
        
            assertNull("No mapping",virtualUserTable.getMappings(user, domain));
        
            assertTrue("Added virtual mapping", addMapping(user, domain, address, ADDRESS_TYPE));
            assertTrue("Added virtual mapping", addMapping(user, domain, address2, ADDRESS_TYPE));

            assertEquals("Two mappings",virtualUserTable.getMappings(user, domain).size(),2);
            assertEquals("One mappingline",virtualUserTable.getAllMappings().size(),1);
        
            assertTrue("remove virtual mapping", removeMapping(user, domain, address, ADDRESS_TYPE));
        
            if (virtualUserTable instanceof JDBCVirtualUserTable) {
                try {
                    assertTrue("Added virtual mapping", addMapping(user, domain, invalidAddress, ADDRESS_TYPE));
                } catch (InvalidMappingException e) {
                    catched = true;
                } 
                assertTrue("Invalid Mapping throw exception" , catched);
            }         
            
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
}
