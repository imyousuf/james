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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.resolver.api.InstanceFactory;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.model.User;

/**
 * UsersStore implementation which will parse the configuration file for users-store and add every configured repository 
 *
 */
public class JamesUsersRepository implements UsersRepository ,org.apache.james.user.api.JamesUsersRepository, Configurable, LogEnabled{

    protected Log log;

    private InstanceFactory factory;
    private HierarchicalConfiguration config;

    private UsersRepository repos;

  
    @Resource(name="instanceFactory")
    public void setInstanceFactory(InstanceFactory factory) {
        this.factory = factory;
    }
    
    public void setLog(Log log) {
        this.log = log;
    }

    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        this.config = config;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        HierarchicalConfiguration repConf = config.configurationAt("repository");
        String repName = repConf.getString("[@name]", null);
        String repClass = repConf.getString("[@class]");

        if (repName == null) {
            repName = repClass;
        }

        if (log.isDebugEnabled()) {
            log.debug("Starting " + repClass);
        }

        repos = (UsersRepository)factory.newInstance(loader.loadClass(repClass), log, repConf);

        if (log.isInfoEnabled()) {
            StringBuffer logBuffer = new StringBuffer(64).append("Bean  ").append(repName).append(" started.");
            log.info(logBuffer.toString());
        }
   
    }

    public boolean addUser(User user) {
        return repos.addUser(user);
    }

    public void addUser(String name, Object attributes) {
        repos.addUser(name, attributes);
    }

    public boolean addUser(String username, String password) {
        return repos.addUser(username, password);
    }

    public User getUserByName(String name) {
        return repos.getUserByName(name);
    }

    public User getUserByNameCaseInsensitive(String name) {
        return repos.getUserByNameCaseInsensitive(name);
    }

    public String getRealName(String name) {
        return repos.getRealName(name);
    }

    public boolean updateUser(User user) {
        return repos.updateUser(user);
    }

    public void removeUser(String name) {
        repos.removeUser(name);
    }

    public boolean contains(String name) {
        return repos.contains(name);
    }

    public boolean containsCaseInsensitive(String name) {
        return repos.containsCaseInsensitive(name);
    }

    public boolean test(String name, String password) {
        return repos.test(name, password);

    }

    public int countUsers() {
        return repos.countUsers();

    }

    public Iterator<String> list() {
        return repos.list();
    }

    public boolean supportVirtualHosting() {
        return repos.supportVirtualHosting();
    }

    public Collection<String> getMappings(String user, String domain) throws ErrorMappingException {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).getMappings(user, domain);
        }
        return null;
    }

    public boolean addRegexMapping(String user, String domain, String regex) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).addRegexMapping(user, domain,regex);
        }        
        return false;
    }

    public boolean removeRegexMapping(String user, String domain, String regex) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).removeRegexMapping(user, domain,regex);
        }        
        return false;
    }

    public boolean addAddressMapping(String user, String domain, String address) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).addAddressMapping(user, domain, address);
        }        
        return false;
    }

    public boolean removeAddressMapping(String user, String domain, String address) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).removeAddressMapping(user, domain, address);
        }        
        return false;
    }

    public boolean addErrorMapping(String user, String domain, String error) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).addErrorMapping(user, domain, error);
        }        
        return false;
    }

    public boolean removeErrorMapping(String user, String domain, String error) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).removeErrorMapping(user, domain, error);
        }        
        return false;
    }

    public Collection<String> getUserDomainMappings(String user, String domain) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).getUserDomainMappings(user, domain);
        }        
        return null;
    }

    public boolean addMapping(String user, String domain, String mapping) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).addMapping(user, domain, mapping);
        }        
        return false;
    }

    public boolean removeMapping(String user, String domain, String mapping) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).removeMapping(user, domain, mapping);
        }        
        return false;
    }

    public Map<String, Collection<String>> getAllMappings() {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).getAllMappings();
        }        
        return new HashMap<String, Collection<String>>();
    }

    public boolean addAliasDomainMapping(String aliasDomain, String realDomain) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).addAliasDomainMapping(aliasDomain, realDomain);
        }        
        return false;
    }

    public boolean removeAliasDomainMapping(String aliasDomain, String realDomain) {
        if (repos instanceof JamesUsersRepository) {
            return ((org.apache.james.user.api.JamesUsersRepository) repos).removeAliasDomainMapping(aliasDomain, realDomain);
        }        
        return false;
    }

    public void setEnableAliases(boolean enableAliases) {
        if (repos instanceof JamesUsersRepository) {
            ((org.apache.james.user.api.JamesUsersRepository) repos).setEnableAliases(enableAliases);
        }        
    }

    public void setEnableForwarding(boolean enableForwarding) {
        if (repos instanceof JamesUsersRepository) {
            ((org.apache.james.user.api.JamesUsersRepository) repos).setEnableForwarding(enableForwarding);
        }  
    }

    public void setIgnoreCase(boolean ignoreCase) {
        if (repos instanceof JamesUsersRepository) {
            ((org.apache.james.user.api.JamesUsersRepository) repos).setIgnoreCase(ignoreCase);
        }  
    }

}
