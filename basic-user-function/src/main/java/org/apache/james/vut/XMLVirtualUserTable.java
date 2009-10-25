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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.impl.vut.AbstractVirtualUserTable;
import org.apache.james.impl.vut.VirtualUserTableUtil;

public class XMLVirtualUserTable extends AbstractVirtualUserTable {
    /**
     * Holds the configured mappings
     */
    private Map<String,String> mappings;
    
    private List<String> domains;
    
    private final static String WILDCARD = "*";
    
    // TODO: REMOVE ME!
    public void init() throws Exception {
        super.init();
    }
    

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#doConfigure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    @SuppressWarnings("unchecked")
    public void doConfigure(HierarchicalConfiguration arg0) throws ConfigurationException {
        List<String> mapConf = arg0.getList("mapping");
    
        mappings = new HashMap<String,String>();
        domains = new ArrayList<String>();
        
        if (mapConf != null && mapConf.size() > 0) {
            for (int i = 0; i < mapConf.size(); i ++) {       
                mappings.putAll(VirtualUserTableUtil.getXMLMappings(mapConf.get(i)));
            }
        } else {
            throw new ConfigurationException("No mapping configured");
        }
        
        // Add domains of the mappings map to the domains List
        Iterator<String> keys = mappings.keySet().iterator();
        
        while (keys.hasNext()) {
            String key = keys.next().toString();

            String[] args1 = key.split("@");
            if (args1 != null && args1.length > 1) {
                String domain = args1[1].toLowerCase();
                if (domains.contains(domain) == false && domain.equals(WILDCARD) == false) {
                    domains.add(domain);
                }
            }
        }
        
        setAutoDetect(arg0.getBoolean("autodetect",true));  
        setAutoDetectIP(arg0.getBoolean("autodetectIP", true));  
        
    }
    
    /**
     * Not implemented
     */
    public boolean addMappingInternal(String user, String domain, String mapping) {
        // Not supported
        return false;
    }

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#mapAddressInternal(java.lang.String, java.lang.String)
     */
    protected String mapAddressInternal(String user, String domain) {
        if (mappings == null) {
            return null;
        } else {
            return VirtualUserTableUtil.getTargetString(user, domain, mappings);
    
        }
    }

    /**
     * Not implemented
     */
    public boolean removeMappingInternal(String user, String domain, String mapping) {
        // Not supported
        return false;
    }

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#getUserDomainMappingsInternal(java.lang.String, java.lang.String)
     */
    public Collection<String> getUserDomainMappingsInternal(String user, String domain) {
        if (mappings == null) {
            return null;
        } else {
            String maps = mappings.get(user + "@" + domain);
            if (maps != null) {
                return VirtualUserTableUtil.mappingToCollection(maps);
            } else {
                return null;
            }
        }
    }

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#getDomainsInternal()
     */
    protected List<String> getDomainsInternal() {
        return domains;
    }

    /**
     * @see org.apache.james.api.domainlist.DomainList#containsDomain(java.lang.String)
     */
    public boolean containsDomain(String domain) {
        if (domains == null) {
            return false;
        } else {
            return domains.contains(domain);
        }
    }

    /**
     * @see org.apache.james.impl.vut.AbstractVirtualUserTable#getAllMappingsInternal()
     */
    public Map<String,Collection<String>> getAllMappingsInternal() {
        if ( mappings != null && mappings.size() > 0) {
            Map<String,Collection<String>> mappingsNew = new HashMap<String,Collection<String>>();
            Iterator<String> maps = mappings.keySet().iterator();
                
            while (maps.hasNext()) {
                String key = maps.next();
                mappingsNew.put(key, VirtualUserTableUtil.mappingToCollection(mappings.get(key).toString()));
            }
            return mappingsNew;
        } else {
            return null;
        }
    }
}
