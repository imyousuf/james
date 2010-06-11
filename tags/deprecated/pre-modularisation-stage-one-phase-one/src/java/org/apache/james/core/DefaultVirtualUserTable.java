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



package org.apache.james.core;

import java.util.Collection;
import java.util.Map;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.VirtualUserTableManagement;
import org.apache.james.services.VirtualUserTableStore;
import org.apache.james.vut.ErrorMappingException;
import org.apache.james.vut.InvalidMappingException;

/**
 * Default VirtualUserTable
 */
public class DefaultVirtualUserTable implements VirtualUserTableManagement, Serviceable, Initializable {

    VirtualUserTableManagement vut = null;
    
    VirtualUserTableStore store = null;
    
    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        vut = (VirtualUserTableManagement) store.getTable("DefaultVirtualUserTable");
        if (vut == null) {
            throw new ServiceException("","The DefaultVirtualUserTable could not be found.");
        }
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        store = (VirtualUserTableStore) arg0.lookup(VirtualUserTableStore.ROLE);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#addAddressMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        return vut.addAddressMapping(user, domain, address);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#addErrorMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        return vut.addErrorMapping(user, domain, error);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#addRegexMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        return vut.addRegexMapping(user, domain, regex);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#getUserDomainMappings(java.lang.String, java.lang.String)
     */
    public Collection getUserDomainMappings(String user, String domain) throws InvalidMappingException {
        return vut.getUserDomainMappings(user, domain);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#removeAddressMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        return vut.removeAddressMapping(user, domain, address);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#removeErrorMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        return vut.removeErrorMapping(user, domain, error);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#removeRegexMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        return vut.removeRegexMapping(user, domain, regex);
    }

    /**
     * @see org.apache.james.services.VirtualUserTable#getMappings(java.lang.String, java.lang.String)
     */
    public Collection getMappings(String user, String domain) throws ErrorMappingException {
        return vut.getMappings(user, domain);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#addMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addMapping(String user, String domain, String mapping) throws InvalidMappingException {
        return vut.addMapping(user, domain, mapping);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#removeMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeMapping(String user, String domain, String mapping) throws InvalidMappingException {
        return vut.removeMapping(user, domain, mapping);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#getAllMappings()
     */
    public Map getAllMappings() {
        return vut.getAllMappings();
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#addAliasDomain(java.lang.String, java.lang.String)
     */
    public boolean addAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException {
        return vut.addAliasDomainMapping(aliasDomain, realDomain);
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagement#removeAliasDomain(java.lang.String, java.lang.String)
     */
    public boolean removeAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException {
        return vut.removeAliasDomainMapping(aliasDomain, realDomain);
    }
}
