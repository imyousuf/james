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

import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.user.api.UsersRepository;

public abstract class AbstractUsersRepository implements UsersRepository, LogEnabled, Configurable{

    private DomainList domainList;
    private boolean virtualHosting;
    private Log logger;


    protected Log getLogger() {
        return logger;
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.api.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log logger) {
        this.logger = logger;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.api.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration configuration) throws ConfigurationException{
       
        virtualHosting = configuration.getBoolean("enableVirtualHosting", false);

        doConfigure(configuration);
    }
   
    
    protected void doConfigure(HierarchicalConfiguration config) throws ConfigurationException{
        
    }

    public void setEnableVirtualHosting(boolean virtualHosting) {
        this.virtualHosting = virtualHosting;
    }
    
    @Resource(name="domainlist")
    public void setDomainList(DomainList domainList) {
        this.domainList = domainList;
    }
    
    protected boolean isValidUsername(String username) {
        int i = username.indexOf("@");
        if (supportVirtualHosting()) {
            // need a @ in the username
            if (i == -1) {
                return false;
            } else {
                String domain = username.substring(i + 1);
                if (domainList.containsDomain(domain) == false) {
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            // @ only allowed when virtualhosting is supported
            if (i != -1) {
                return false;
            }
        }
        return true;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UsersRepository#addUser(java.lang.String, java.lang.String)
     */
    public boolean addUser(String username, String password) {
        
        if (contains(username) == false && isValidUsername(username)) {
            return doAddUser(username, password);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UsersRepository#supportVirtualHosting()
     */
    public boolean supportVirtualHosting() {
        return virtualHosting;
    }
    
    /**
     * Add the user with the given username and password
     * 
     * @param username
     * @param password
     * @return successful
     */
    protected abstract boolean doAddUser(String username, String password);
}
