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

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.services.VirtualUserTableManagement;

import junit.framework.TestCase;

public abstract class AbstractVirtualUserTableTest extends TestCase {

    protected VirtualUserTableManagement virtualUserTable;
    protected void setUp() throws Exception {
        virtualUserTable = getVirtalUserTable();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        ContainerUtil.dispose(virtualUserTable);
    }
    
    protected abstract VirtualUserTableManagement getVirtalUserTable() throws ServiceException, ConfigurationException, Exception;
    

    public void testStoreAndRetrieveRegexMapping() throws ErrorMappingException {
    
        String user = "test";
        String domain = "localhost";
        String regex = "(.*):{$1}@localhost";
        String regex2 = "(.+):{$1}@test"; 
        String invalidRegex = ".*):";
        boolean catched = false;
        try {
        
            assertTrue("No mapping",virtualUserTable.getMappings(user, domain).isEmpty());
        
            assertTrue("Added virtual mapping", virtualUserTable.addRegexMapping(user, domain, regex));
            assertTrue("Added virtual mapping", virtualUserTable.addRegexMapping(user, domain, regex2));

            assertTrue("Two mappings",virtualUserTable.getMappings(user, domain).size() == 2);
        
            assertTrue("remove virtual mapping", virtualUserTable.removeRegexMapping(user, domain, regex));
        
            try {
                assertTrue("Added virtual mapping", virtualUserTable.addRegexMapping(user, domain, invalidRegex));
            } catch (InvalidMappingException e) {
                catched = true;
            }
            assertTrue("Invalid Mapping throw exception" , catched);

            assertTrue("remove virtual mapping", virtualUserTable.removeRegexMapping(user, domain, regex2));
            assertTrue("No mapping",virtualUserTable.getMappings(user, domain).isEmpty());
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
        
            assertTrue("No mapping",virtualUserTable.getMappings(user, domain).isEmpty());
        
            assertTrue("Added virtual mapping", virtualUserTable.addAddressMapping(user, domain, address));
            assertTrue("Added virtual mapping", virtualUserTable.addAddressMapping(user, domain, address2));

            assertTrue("Two mappings",virtualUserTable.getMappings(user, domain).size() == 2);
        
            assertTrue("remove virtual mapping", virtualUserTable.removeAddressMapping(user, domain, address));
        
            try {
                assertTrue("Added virtual mapping", virtualUserTable.addAddressMapping(user, domain, invalidAddress));
            } catch (InvalidMappingException e) {
                catched = true;
            }
            assertTrue("Invalid Mapping throw exception" , catched);

            assertTrue("remove virtual mapping", virtualUserTable.removeAddressMapping(user, domain, address2));
            assertTrue("No mapping",virtualUserTable.getMappings(user, domain).isEmpty());
        } catch (InvalidMappingException e) {
            fail("Storing failed");
        }
    
    }

    public void testStoreAndRetrieveErrorMapping() throws ErrorMappingException {
    
        String user = "test";
        String domain = "localhost";
        String error = "Bounce!";
        boolean catched = false;
        try {
        
            assertTrue("No mapping",virtualUserTable.getMappings(user, domain).isEmpty());
        
            assertTrue("Added virtual mapping", virtualUserTable.addErrorMapping(user, domain, error));

            try {
                virtualUserTable.getMappings(user, domain);
            } catch (ErrorMappingException e) {
                catched = true;
            }
            assertTrue("Error Mapping throw exception" , catched);

            assertTrue("remove virtual mapping", virtualUserTable.removeErrorMapping(user, domain, error));
            assertTrue("No mapping",virtualUserTable.getMappings(user, domain).isEmpty());
        } catch (InvalidMappingException e) {
             fail("Storing failed");
        }

    
    }

}
