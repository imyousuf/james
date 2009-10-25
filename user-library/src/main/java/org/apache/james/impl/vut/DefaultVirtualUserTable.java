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



package org.apache.james.impl.vut;

import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.james.api.vut.management.InvalidMappingException;
import org.apache.james.api.vut.management.VirtualUserTableManagement;

/**
 * Default VirtualUserTable
 */
public class DefaultVirtualUserTable implements VirtualUserTableManagement {

    VirtualUserTableManagement vut = null;
    
    VirtualUserTableStore store = null;
    
    @Resource(name="org.apache.james.api.vut.VirtualUserTableStore")
    public void setVirtualUserTableStore(VirtualUserTableStore store) {
        this.store = store;
    }

    @PostConstruct
    public void init() throws Exception {
        vut = (VirtualUserTableManagement) store.getTable("DefaultVirtualUserTable");
        if (vut == null) {
            throw new RuntimeException("The DefaultVirtualUserTable could not be found.");
        }
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#addAddressMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        return vut.addAddressMapping(user, domain, address);
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#addErrorMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        return vut.addErrorMapping(user, domain, error);
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#addRegexMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        return vut.addRegexMapping(user, domain, regex);
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#getUserDomainMappings(java.lang.String, java.lang.String)
     */
    public Collection<String> getUserDomainMappings(String user, String domain) throws InvalidMappingException {
        return vut.getUserDomainMappings(user, domain);
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#removeAddressMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        return vut.removeAddressMapping(user, domain, address);
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#removeErrorMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        return vut.removeErrorMapping(user, domain, error);
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#removeRegexMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        return vut.removeRegexMapping(user, domain, regex);
    }

    /**
     * @see org.apache.james.api.vut.VirtualUserTable#getMappings(java.lang.String, java.lang.String)
     */
    public Collection<String> getMappings(String user, String domain) throws ErrorMappingException {
        return vut.getMappings(user, domain);
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#addMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addMapping(String user, String domain, String mapping) throws InvalidMappingException {
        return vut.addMapping(user, domain, mapping);
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#removeMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeMapping(String user, String domain, String mapping) throws InvalidMappingException {
        return vut.removeMapping(user, domain, mapping);
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#getAllMappings()
     */
    public Map<String,Collection<String>> getAllMappings() {
        return vut.getAllMappings();
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#addAliasDomainMapping(String, String)
     */
    public boolean addAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException {
        return vut.addAliasDomainMapping(aliasDomain, realDomain);
    }

    /**
     * @see org.apache.james.api.vut.management.VirtualUserTableManagement#removeAliasDomainMapping(String, String)
     */
    public boolean removeAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException {
        return vut.removeAliasDomainMapping(aliasDomain, realDomain);
    }
}
