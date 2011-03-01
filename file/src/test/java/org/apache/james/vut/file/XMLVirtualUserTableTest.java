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
package org.apache.james.vut.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.james.vut.api.VirtualUserTable;
import org.apache.james.vut.api.VirtualUserTableException;
import org.apache.james.vut.file.XMLVirtualUserTable;
import org.apache.james.vut.lib.AbstractVirtualUserTable;
import org.apache.james.vut.lib.AbstractVirtualUserTableTest;
import org.apache.james.vut.lib.VirtualUserTableUtil;
import org.slf4j.LoggerFactory;

/**
 * Test the XML Virtual User Table implementation.
 */
public class XMLVirtualUserTableTest extends AbstractVirtualUserTableTest {

    private DefaultConfigurationBuilder defaultConfiguration = new DefaultConfigurationBuilder();
    
    @Override
    protected void setUp() throws Exception {
        defaultConfiguration.setDelimiterParsingDisabled(true);
        super.setUp();
    }

    protected AbstractVirtualUserTable getVirtualUserTable() throws Exception {
        XMLVirtualUserTable virtualUserTable = new XMLVirtualUserTable();
        virtualUserTable.setLog(LoggerFactory.getLogger("MockLog"));
        return virtualUserTable;
    }
    
    /**
     * @throws VirtualUserTableException 
     * @see org.apache.james.vut.lib.AbstractVirtualUserTableTest#addMapping(java.lang.String, java.lang.String, java.lang.String, int)
     */
    protected boolean addMapping(String user, String domain, String mapping, int type) throws VirtualUserTableException {

        Collection<String> mappings = virtualUserTable.getUserDomainMappings(user, domain);

        if (mappings == null) {
            mappings = new ArrayList<String>();
        } else {
            removeMappingsFromConfig(user,domain,mappings);
        }
    
        if (type == ERROR_TYPE) {
            mappings.add(VirtualUserTable.ERROR_PREFIX + mapping);
        } else if (type == REGEX_TYPE) {
            mappings.add(VirtualUserTable.REGEX_PREFIX + mapping);
        } else if (type == ADDRESS_TYPE) {
            mappings.add(mapping);
        }  else if (type == ALIASDOMAIN_TYPE) {
            mappings.add(VirtualUserTable.ALIASDOMAIN_PREFIX + mapping);
        }
        
        if (mappings.size() > 0) { 
            defaultConfiguration.addProperty("mapping",user + "@" + domain + "=" + VirtualUserTableUtil.CollectionToMapping(mappings));
        }
    
        try {
            virtualUserTable.configure(defaultConfiguration);
            } catch (Exception e) {
            if (mappings.size() > 0) {
                return false;
            } else {
                return true;
            }
        }
            
        return true;
    
    }

    /**
     * @throws VirtualUserTableException 
     * @see org.apache.james.vut.lib.AbstractVirtualUserTableTest#removeMapping(java.lang.String, java.lang.String, java.lang.String, int)
     */
    protected boolean removeMapping(String user, String domain, String mapping, int type) throws VirtualUserTableException  {       

        Collection<String> mappings = virtualUserTable.getUserDomainMappings(user, domain);
        
        if (mappings == null) {
            return false;
        }
    
        removeMappingsFromConfig(user,domain, mappings);
    
        if (type == ERROR_TYPE) {
            mappings.remove(VirtualUserTable.ERROR_PREFIX + mapping);
        } else if (type == REGEX_TYPE) {
            mappings.remove(VirtualUserTable.REGEX_PREFIX + mapping);
        } else if (type == ADDRESS_TYPE) {
            mappings.remove(mapping);    
        }  else if (type == ALIASDOMAIN_TYPE) {
            mappings.remove(VirtualUserTable.ALIASDOMAIN_PREFIX + mapping);
        }

        if (mappings.size() > 0) {
            defaultConfiguration.addProperty("mapping",user + "@" + domain +"=" + VirtualUserTableUtil.CollectionToMapping(mappings));
        } 
    
        try {
            virtualUserTable.configure(defaultConfiguration);
            } catch (Exception e) {
           if (mappings.size() > 0) {
               return false;
           } else {
               return true;
           }
        }
            
        return true;

    }
    
    @SuppressWarnings("unchecked")
    private void removeMappingsFromConfig(String user, String domain, Collection<String> mappings) {
        List<String> confs = defaultConfiguration.getList("mapping");
        List<String> stored = new ArrayList<String>();
        for (int i = 0; i < confs.size(); i++) {
            String c = confs.get(i);
            String mapping = user + "@" + domain + "=" + VirtualUserTableUtil.CollectionToMapping(mappings);
                        
            if (!c.equalsIgnoreCase(mapping)){
                stored.add(c);
            }
        }
        // clear old values
        defaultConfiguration.clear();
        // add stored mappings
        for (int i = 0; i < stored.size(); i++) {
            defaultConfiguration.addProperty("mapping", stored.get(i));
        }
    }

}
