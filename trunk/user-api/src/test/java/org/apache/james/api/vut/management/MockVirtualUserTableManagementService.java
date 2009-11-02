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
package org.apache.james.api.vut.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.james.api.vut.management.VirtualUserTableManagementException;
import org.apache.james.api.vut.management.VirtualUserTableManagementService;

public class MockVirtualUserTableManagementService implements VirtualUserTableManagementService{
    private Map<String,Collection<String>> mappings = new HashMap<String, Collection<String>>();

    public boolean addAddressMapping(String virtualUserTable, String user,
            String domain, String address)
            throws VirtualUserTableManagementException {
        Collection<String> map = mappings.get(user + "@" + domain);
        if (map == null) {
            map = new ArrayList<String>();
            mappings.put(user + "@" + domain, map);

        } else {
            if (map.contains(address)) return false;
        }
        return map.add(address);
    }

    public boolean addAliasDomainMapping(String virtualUserTable,
            String aliasDomain, String realDomain)
            throws VirtualUserTableManagementException {
        Collection<String> map = mappings.get(realDomain);
        if (map == null) {
            map = new ArrayList<String>();
            mappings.put(realDomain, map);

        } else {
            if (map.contains(aliasDomain)) return false;
        }
        return map.add(aliasDomain);
    }

    public boolean addErrorMapping(String virtualUserTable, String user,
            String domain, String error)
            throws VirtualUserTableManagementException {
        Collection<String> map = mappings.get(user + "@" + domain);
        if (map == null) {
            map = new ArrayList<String>();
            mappings.put(user + "@" + domain, map);
        } else {
            if (map.contains(error)) return false;
        }
        return map.add(error);
    }

    public boolean addMapping(String virtualUserTable, String user,
            String domain, String mapping)
            throws VirtualUserTableManagementException {
        Collection<String> map = mappings.get(user + "@" + domain);
        if (map == null) {
            map = new ArrayList<String>();           
            mappings.put(user + "@" + domain, map);
        } else {
            if (map.contains(mapping)) return false;
        }     
        return map.add(mapping);
    }

    public boolean addRegexMapping(String virtualUserTable, String user,
            String domain, String regex)
            throws VirtualUserTableManagementException {
        Collection<String> map = mappings.get(user + "@" + domain);
        if (map == null) {
            map = new ArrayList<String>();
            mappings.put(user + "@" + domain, map);
        } else {
            if (map.contains(regex)) return false;
        }     
        return map.add(regex);
    }

    public Map<String, Collection<String>> getAllMappings(
            String virtualUserTable) throws VirtualUserTableManagementException {
        return mappings;
    }

    public Collection<String> getUserDomainMappings(String virtualUserTable,
            String user, String domain)
            throws VirtualUserTableManagementException {
        return mappings.get(user + "@" + domain);
    }

    public boolean removeAddressMapping(String virtualUserTable, String user,
            String domain, String address)
            throws VirtualUserTableManagementException {
        Collection<String> col = mappings.get(user + "@" + domain);
        Iterator<String> it = col.iterator();
        while (it.hasNext()) {
            String mapping = it.next();
            if (mapping.equalsIgnoreCase(address)) {
                return col.remove(address);  
            }
        }
        return false;
    }

    public boolean removeAliasDomainMapping(String virtualUserTable,
            String aliasDomain, String realDomain)
            throws VirtualUserTableManagementException {
        Collection<String> col = mappings.get(realDomain);
        Iterator<String> it = col.iterator();
        while (it.hasNext()) {
            String mapping = it.next();
            if (mapping.equalsIgnoreCase(aliasDomain)) {
                return col.remove(aliasDomain);  
            }
        }
        return false;
    }

    public boolean removeErrorMapping(String virtualUserTable, String user,
            String domain, String error)
            throws VirtualUserTableManagementException {
        Collection<String> col = mappings.get(user + "@" + domain);
        Iterator<String> it = col.iterator();
        while (it.hasNext()) {
            String mapping = it.next();
            if (mapping.equalsIgnoreCase(error)) {
                return col.remove(error);  
            }
        }
        return false;
    }

    public boolean removeMapping(String virtualUserTable, String user,
            String domain, String rawmapping)
            throws VirtualUserTableManagementException {
        Collection<String> col = mappings.get(user + "@" + domain);
        Iterator<String> it = col.iterator();
        while (it.hasNext()) {
            String mapping = it.next();
            if (mapping.equalsIgnoreCase(rawmapping)) {
                return col.remove(rawmapping);  
            }
        }
        return false;
    }

    public boolean removeRegexMapping(String virtualUserTable, String user,
            String domain, String regex)
            throws VirtualUserTableManagementException {
        Collection<String> col = mappings.get(user + "@" + domain);
        Iterator<String> it = col.iterator();
        while (it.hasNext()) {
            String mapping = it.next();
            if (mapping.equalsIgnoreCase(regex)) {
                return col.remove(regex);  
            }
        }
        return false;
    }

}
