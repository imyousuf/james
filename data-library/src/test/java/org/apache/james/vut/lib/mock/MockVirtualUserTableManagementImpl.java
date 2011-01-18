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


package org.apache.james.vut.lib.mock;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.james.vut.api.VirtualUserTable;
import org.apache.james.vut.api.VirtualUserTableException;
import org.apache.james.vut.lib.VirtualUserTableUtil;

public class MockVirtualUserTableManagementImpl implements VirtualUserTable {

    HashMap store = new HashMap();
    
    public void addAddressMapping(String user, String domain, String address) throws VirtualUserTableException {
        addRawMapping(user,domain,address);
    }

    public void addErrorMapping(String user, String domain, String error) throws VirtualUserTableException{
        addRawMapping(user,domain,VirtualUserTable.ERROR_PREFIX + error);
    }

    public void addMapping(String user, String domain, String mapping) throws VirtualUserTableException{
        if (mapping.startsWith(VirtualUserTable.ERROR_PREFIX)){
            addErrorMapping(user,domain,mapping.substring(VirtualUserTable.ERROR_PREFIX.length()));
        } else if (mapping.startsWith(VirtualUserTable.REGEX_PREFIX)) {
            addErrorMapping(user,domain,mapping.substring(VirtualUserTable.REGEX_PREFIX.length()));
        } else {
            addAddressMapping(user,domain,mapping);
        }
    }

    public void addRegexMapping(String user, String domain, String regex) throws VirtualUserTableException {
        addRawMapping(user,domain,VirtualUserTable.REGEX_PREFIX + regex);
    }

    public Map getAllMappings() throws VirtualUserTableException {
        if (store.size() > 0) {
            return store;
        } else {
            return null;
        }
    }

    public Collection getUserDomainMappings(String user, String domain) throws VirtualUserTableException{
        String mapping = (String) store.get(user + "@" + domain);
        if (mapping != null) {
            return VirtualUserTableUtil.mappingToCollection(mapping);
        } else {
            return null;
        }
    }

    public void removeAddressMapping(String user, String domain, String address) throws VirtualUserTableException {
        removeRawMapping(user,domain,address);
    }

    public void removeErrorMapping(String user, String domain, String error) throws VirtualUserTableException {
        removeRawMapping(user,domain,VirtualUserTable.ERROR_PREFIX + error);
    }

    public void removeMapping(String user, String domain, String mapping) throws VirtualUserTableException{
        if (mapping.startsWith(VirtualUserTable.ERROR_PREFIX)){
            removeErrorMapping(user,domain,mapping.substring(VirtualUserTable.ERROR_PREFIX.length()));
        } else if (mapping.startsWith(VirtualUserTable.REGEX_PREFIX)) {
            removeErrorMapping(user,domain,mapping.substring(VirtualUserTable.REGEX_PREFIX.length()));
        } else {
            removeAddressMapping(user,domain,mapping);
        }
    }

    public void removeRegexMapping(String user, String domain, String regex) throws VirtualUserTableException  {
        removeRawMapping(user,domain,VirtualUserTable.REGEX_PREFIX + regex);
    }

    public Collection<String> getMappings(String user, String domain) throws ErrorMappingException, VirtualUserTableException{
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    private void addRawMapping(String user,String domain, String mapping) throws VirtualUserTableException {
        Collection map;
        String key = user + "@" + domain;
        String mappings = (String) store.get(key);
    
        if (mappings != null) {
            map = VirtualUserTableUtil.mappingToCollection(mappings);
            
            if (map.contains(mapping)) {
                throw new VirtualUserTableException("Mapping " + mapping + " already exist!");
            } else {
                map.add(mapping);
                store.put(key, VirtualUserTableUtil.CollectionToMapping(map));
            }
        } else {
            store.put(key, mapping);
        }
    }
    
    private void removeRawMapping(String user,String domain, String mapping) throws VirtualUserTableException {
        Collection map;
        String key = user + "@" + domain;
        String mappings = (String) store.get(key);
        if (mappings != null) {
            map = VirtualUserTableUtil.mappingToCollection(mappings);
            if (map.remove(mapping)) {
                store.put(key, VirtualUserTableUtil.CollectionToMapping(map));
            }
        }
        throw new VirtualUserTableException("Mapping does not exist");
    }

    public void addAliasDomainMapping(String aliasDomain, String realDomain) throws VirtualUserTableException  {
        addRawMapping(null,aliasDomain,VirtualUserTable.ALIASDOMAIN_PREFIX + realDomain);
    }

    public void removeAliasDomainMapping(String aliasDomain, String realDomain) throws VirtualUserTableException {
        removeRawMapping(null,aliasDomain,VirtualUserTable.ALIASDOMAIN_PREFIX + realDomain);
    }

}
