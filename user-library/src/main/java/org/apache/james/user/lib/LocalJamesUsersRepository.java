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

package org.apache.james.user.lib;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.user.api.JamesUsersRepository;

import java.util.Collection;
import java.util.Map;

/**
 * This is a wrapper that provide access to the "LocalUsers" repository
 * but expect to find a JamesUsersRepository and return an object implementing
 * this extended interface
 * 
 * @deprecated use {@link LocalUsersRepository}
 */
@Deprecated
public class LocalJamesUsersRepository extends LocalUsersRepository implements JamesUsersRepository{

    
    private JamesUsersRepository getUsersRepository() {
        if (users instanceof JamesUsersRepository) {
            return (JamesUsersRepository) users;
        }
        throw new UnsupportedOperationException("No supported by this implementation");
    }


    @Override
    public void init() throws Exception {
        super.init();
        
        if ((users instanceof JamesUsersRepository) == false) {
            throw new ConfigurationException("Please use the LocalUsersRepository for this UsersRepository");
        }
    }


    /**
     * @see org.apache.james.user.api.JamesUsersRepository#setEnableAliases(boolean)
     */
    public void setEnableAliases(boolean enableAliases) {
        getUsersRepository().setEnableAliases(enableAliases);
    }

    /**
     * @see org.apache.james.user.api.JamesUsersRepository#setEnableForwarding(boolean)
     */
    public void setEnableForwarding(boolean enableForwarding) {
        getUsersRepository().setEnableForwarding(enableForwarding);
    }

    /**
     * @see org.apache.james.user.api.JamesUsersRepository#setIgnoreCase(boolean)
     */
    public void setIgnoreCase(boolean ignoreCase) {
        getUsersRepository().setIgnoreCase(ignoreCase);
    }

    /**
     * @see org.apache.james.vut.api.VirtualUserTable#getMappings(java.lang.String, java.lang.String)
     */
    public Collection<String> getMappings(String user, String domain) throws ErrorMappingException {
        return getUsersRepository().getMappings(user, domain);
    }

    public boolean addAddressMapping(String user, String domain, String address) {
        return getUsersRepository().addAddressMapping(user, domain, address);
    }

    public boolean addAliasDomainMapping(String aliasDomain, String realDomain) {
        return getUsersRepository().addAliasDomainMapping(aliasDomain, realDomain);
    }

    public boolean addErrorMapping(String user, String domain, String error) {
        return getUsersRepository().addErrorMapping(user, domain, error);
    }

    public boolean addMapping(String user, String domain, String mapping) {
        return getUsersRepository().addMapping(user, domain, mapping);
    }

    public boolean addRegexMapping(String user, String domain, String regex) {
        return getUsersRepository().addRegexMapping(user, domain, regex);
    }

    public Map<String, Collection<String>> getAllMappings() {
        return getUsersRepository().getAllMappings();
    }

    public Collection<String> getUserDomainMappings(String user, String domain) {
        return getUsersRepository().getUserDomainMappings(user, domain);
    }

    public boolean removeAddressMapping(String user, String domain, String address) {
        return getUsersRepository().removeAddressMapping(user, domain, address);
    }

    public boolean removeAliasDomainMapping(String aliasDomain, String realDomain) {
        return getUsersRepository().removeAliasDomainMapping(aliasDomain, realDomain);
    }

    public boolean removeErrorMapping(String user, String domain, String error) {
        return getUsersRepository().removeErrorMapping(user, domain, error);
    }

    public boolean removeMapping(String user, String domain, String mapping) {
        return getUsersRepository().removeMapping(user, domain, mapping);
    }

    public boolean removeRegexMapping(String user, String domain, String regex) {
        return getUsersRepository().removeRegexMapping(user, domain, regex);
    }

}
