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
package org.apache.james.vut.lib;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.james.lifecycle.api.LifecycleUtil;
import org.apache.james.vut.api.VirtualUserTable;
import org.apache.james.vut.api.VirtualUserTable.ErrorMappingException;
import org.apache.james.vut.api.VirtualUserTableException;
import org.apache.james.vut.lib.AbstractVirtualUserTable;

/**
 * The abstract test for the virtual user table.
 * Contains tests related to simple, regexp, wildcard, error,...
 * Extend this and instanciate the needed virtualUserTable implementation.
 */
public abstract class AbstractVirtualUserTableTest extends TestCase {

    protected AbstractVirtualUserTable virtualUserTable;
    
    protected final static int REGEX_TYPE = 0;
    protected final static int ERROR_TYPE = 1;
    protected final static int ADDRESS_TYPE = 2;
    protected final static int ALIASDOMAIN_TYPE = 3;

    protected void setUp() throws Exception {
        virtualUserTable = getVirtualUserTable();
    }

    protected void tearDown() throws Exception {
        
        Map<String,Collection<String>> mappings = virtualUserTable.getAllMappings();

        if (mappings != null) {
            
            Iterator<String> mappingsIt = virtualUserTable.getAllMappings().keySet().iterator();

            while(mappingsIt.hasNext()) {
                String key = mappingsIt.next().toString();
                String args[] = key.split("@");

                Collection<String> map = mappings.get(key);

                Iterator<String> mapIt = map.iterator();

                while (mapIt.hasNext()) {
                    try {
                        removeMapping(args[0], args[1], mapIt.next().toString());
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        LifecycleUtil.dispose(virtualUserTable);
    
    }

    public void testStoreAndRetrieveRegexMapping() throws org.apache.james.vut.api.VirtualUserTable.ErrorMappingException, VirtualUserTableException {
        
        String user = "test";
        String domain = "localhost";
        // String regex = "(.*):{$1}@localhost";
        // String regex2 = "(.+):{$1}@test"; 
        String regex = "(.*)@localhost";
        String regex2 = "(.+)@test"; 
        String invalidRegex = ".*):";
        boolean catched = false;
        
        try {

            assertNull("No mapping",virtualUserTable.getMappings(user, domain));

            assertTrue("Added virtual mapping", addMapping(user, domain, regex, REGEX_TYPE));
            assertTrue("Added virtual mapping", addMapping(user, domain, regex2, REGEX_TYPE));
            assertEquals("Two mappings", virtualUserTable.getMappings(user, domain).size(), 2);
            assertEquals("One mappingline", virtualUserTable.getAllMappings().size(), 1);

            assertTrue("remove virtual mapping", removeMapping(user, domain, regex, REGEX_TYPE));

            try {
                virtualUserTable.addRegexMapping(user, domain, invalidRegex);
            } catch (VirtualUserTableException e) {
                catched = true;
            }
            assertTrue("Invalid Mapping throw exception" , catched);

            assertTrue("remove virtual mapping", removeMapping(user, domain, regex2, REGEX_TYPE));

            assertNull("No mapping", virtualUserTable.getMappings(user, domain));

            assertNull("No mappings", virtualUserTable.getAllMappings());

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail("Storing failed");
        }

    }


    public void testStoreAndRetrieveAddressMapping() throws ErrorMappingException, VirtualUserTableException {
        
        String user = "test";
        String domain = "localhost";
        String address = "test@localhost2";
        String address2 = "test@james";

        try {

            assertNull("No mapping",virtualUserTable.getMappings(user, domain));

            assertTrue("Added virtual mapping", addMapping(user, domain, address, ADDRESS_TYPE));
            assertTrue("Added virtual mapping", addMapping(user, domain, address2, ADDRESS_TYPE));

            assertEquals("Two mappings", virtualUserTable.getMappings(user, domain).size(),2);
            assertEquals("One mappingline", virtualUserTable.getAllMappings().size(),1);

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

            assertNull("No mapping", virtualUserTable.getMappings(user, domain));
            assertNull("No mappings", virtualUserTable.getAllMappings());

        } catch (IllegalArgumentException e) {
            fail("Storing failed");
        }


    }

    public void testStoreAndRetrieveErrorMapping() throws ErrorMappingException, VirtualUserTableException {
        
        String user = "test";
        String domain = "localhost";
        String error = "bounce!";
        boolean catched = false;

        try {

            assertNull("No mapping",virtualUserTable.getMappings(user, domain));

            assertTrue("Added virtual mapping", addMapping(user, domain, error, ERROR_TYPE));
            assertEquals("One mappingline", virtualUserTable.getAllMappings().size(),1);

            try {
                virtualUserTable.getMappings(user, domain);
            } catch (ErrorMappingException e) {
                catched = true;
            }
            assertTrue("Error Mapping throw exception" , catched);

            assertTrue("remove virtual mapping", removeMapping(user, domain, error, ERROR_TYPE));
            assertNull("No mapping", virtualUserTable.getMappings(user, domain));
            assertNull("No mappings", virtualUserTable.getAllMappings());

        } catch (IllegalArgumentException e) {
            fail("Storing failed");
        }

    }

    public void testStoreAndRetrieveWildCardAddressMapping() throws ErrorMappingException, VirtualUserTableException {

        String user = "test";
        String user2 = "test2";
        String domain = "localhost";
        String address = "test@localhost2";
        String address2 = "test@james";

        try {

            assertNull("No mapping", virtualUserTable.getMappings(user, domain));

            assertTrue("Added virtual mapping", addMapping(VirtualUserTable.WILDCARD, domain, address, ADDRESS_TYPE));
            assertTrue("Added virtual mapping", addMapping(user, domain, address2, ADDRESS_TYPE));

            assertTrue("One mappings", virtualUserTable.getMappings(user, domain).size() == 1);
            assertTrue("One mappings", virtualUserTable.getMappings(user2, domain).size() == 1);

            assertTrue("remove virtual mapping", removeMapping(user, domain, address2, ADDRESS_TYPE));
            assertTrue("remove virtual mapping", removeMapping(VirtualUserTable.WILDCARD, domain, address, ADDRESS_TYPE));
            assertNull("No mapping", virtualUserTable.getMappings(user, domain));
            assertNull("No mapping", virtualUserTable.getMappings(user2, domain));

        } catch (IllegalArgumentException e) {
            fail("Storing failed");
        }

    }

    public void testRecursiveMapping() throws ErrorMappingException, VirtualUserTableException {
        
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

        } catch (IllegalArgumentException e) {
            fail("Storing failed");
        }
    }


    public void testAliasDomainMapping() throws ErrorMappingException, VirtualUserTableException {
        
        String domain = "realdomain";
        String aliasDomain = "aliasdomain";
        String user = "user";
        String user2 = "user2";

        assertNull("No mappings",virtualUserTable.getAllMappings());
        
        try {
            
            assertTrue("Add mapping",addMapping(VirtualUserTable.WILDCARD, aliasDomain, user2 + "@" + domain, ADDRESS_TYPE));
            assertTrue("Add aliasDomain mapping", addMapping(VirtualUserTable.WILDCARD, aliasDomain, domain, ALIASDOMAIN_TYPE));

            Iterator<String> mappings = virtualUserTable.getMappings(user, aliasDomain).iterator();
            assertEquals("Domain mapped as first ", mappings.next(), user + "@" + domain);
            assertEquals("Address mapped as second ", mappings.next(), user2 + "@" + domain);

            assertTrue("Remove mapping", removeMapping(VirtualUserTable.WILDCARD, aliasDomain, user2 + "@" + domain, ADDRESS_TYPE));
            assertTrue("Remove aliasDomain mapping", removeMapping(VirtualUserTable.WILDCARD, aliasDomain, domain, ALIASDOMAIN_TYPE));

        } catch (IllegalArgumentException e) {
            fail("Storing failed");
        }

    }

    protected abstract AbstractVirtualUserTable getVirtualUserTable() throws Exception;

    protected abstract boolean addMapping(String user , String domain, String mapping,int type) throws VirtualUserTableException;

    protected abstract boolean removeMapping(String user, String domain, String mapping, int type) throws VirtualUserTableException;

    private void removeMapping(String user, String domain, String rawMapping) throws VirtualUserTableException {
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

}
