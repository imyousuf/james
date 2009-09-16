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


package org.apache.james.test.mock.james;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.management.InvalidMappingException;
import org.apache.james.api.vut.management.VirtualUserTableManagement;
import org.apache.james.impl.vut.VirtualUserTableUtil;

public class MockVirtualUserTableManagementImpl implements VirtualUserTableManagement {

    HashMap store = new HashMap();
    
    public boolean addAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        return addRawMapping(user,domain,address);
    }

    public boolean addErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        return addRawMapping(user,domain,VirtualUserTable.ERROR_PREFIX + error);
    }

    public boolean addMapping(String user, String domain, String mapping) throws InvalidMappingException {
        if (mapping.startsWith(VirtualUserTable.ERROR_PREFIX)){
            return addErrorMapping(user,domain,mapping.substring(VirtualUserTable.ERROR_PREFIX.length()));
        } else if (mapping.startsWith(VirtualUserTable.REGEX_PREFIX)) {
            return addErrorMapping(user,domain,mapping.substring(VirtualUserTable.REGEX_PREFIX.length()));
        } else {
            return addAddressMapping(user,domain,mapping);
        }
    }

    public boolean addRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        return addRawMapping(user,domain,VirtualUserTable.REGEX_PREFIX + regex);
    }

    public Map getAllMappings() {
        if (store.size() > 0) {
            return store;
        } else {
            return null;
        }
    }

    public Collection getUserDomainMappings(String user, String domain) throws InvalidMappingException {
        String mapping = (String) store.get(user + "@" + domain);
        if (mapping != null) {
            return VirtualUserTableUtil.mappingToCollection(mapping);
        } else {
            return null;
        }
    }

    public boolean removeAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        return removeRawMapping(user,domain,address);
    }

    public boolean removeErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        return removeRawMapping(user,domain,VirtualUserTable.ERROR_PREFIX + error);
    }

    public boolean removeMapping(String user, String domain, String mapping) throws InvalidMappingException {
        if (mapping.startsWith(VirtualUserTable.ERROR_PREFIX)){
            return removeErrorMapping(user,domain,mapping.substring(VirtualUserTable.ERROR_PREFIX.length()));
        } else if (mapping.startsWith(VirtualUserTable.REGEX_PREFIX)) {
            return removeErrorMapping(user,domain,mapping.substring(VirtualUserTable.REGEX_PREFIX.length()));
        } else {
            return removeAddressMapping(user,domain,mapping);
        }
    }

    public boolean removeRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        return removeRawMapping(user,domain,VirtualUserTable.REGEX_PREFIX + regex);
    }

    public Collection<String> getMappings(String user, String domain) throws ErrorMappingException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    private boolean addRawMapping(String user,String domain, String mapping) {
        Collection map;
        String key = user + "@" + domain;
        String mappings = (String) store.get(key);
    
        if (mappings != null) {
            map = VirtualUserTableUtil.mappingToCollection(mappings);
            
            if (map.contains(mapping)) {
                return false;       
            } else {
                map.add(mapping);
                store.put(key, VirtualUserTableUtil.CollectionToMapping(map));
                return true;
            }
        } else {
            store.put(key, mapping);
            return true;
        }
    }
    
    private boolean removeRawMapping(String user,String domain, String mapping) {
        Collection map;
        String key = user + "@" + domain;
        String mappings = (String) store.get(key);
        if (mappings != null) {
            map = VirtualUserTableUtil.mappingToCollection(mappings);
            if (map.remove(mapping)) {
                store.put(key, VirtualUserTableUtil.CollectionToMapping(map));
                return true;
            }
        }
        return false;
    }

    public boolean addAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException {
    return addRawMapping(null,aliasDomain,VirtualUserTable.ALIASDOMAIN_PREFIX + realDomain);
    }

    public boolean removeAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException {
        return removeRawMapping(null,aliasDomain,VirtualUserTable.ALIASDOMAIN_PREFIX + realDomain);
    }

}
