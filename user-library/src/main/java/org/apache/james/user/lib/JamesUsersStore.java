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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.services.InstanceFactory;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersStore;

/**
 * UsersStore implementation which will parse the configuration file for users-store and add every configured repository 
 *
 */
public class JamesUsersStore implements UsersStore {

    private String defaultName;

    protected Log log;

    protected final Map<String, UsersRepository> objects = Collections.synchronizedMap(new HashMap<String, UsersRepository>());
    private InstanceFactory factory;
    private HierarchicalConfiguration config;

  
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
        List<HierarchicalConfiguration> repConfs = config.configurationsAt("repository");
        for (int i = 0; i < repConfs.size(); i++) {
            final HierarchicalConfiguration repConf = repConfs.get(i);
            String repName = repConf.getString("[@name]", null);
            String repClass = repConf.getString("[@class]");

            if (repName == null) {
                repName = repClass;
            }
            
            if (log.isDebugEnabled()) {
                log.debug("Starting " + repClass);
            }
                        
            
            objects.put(repName, (UsersRepository) factory.newInstance(loader.loadClass(repClass), log, repConf));
            
            if (log.isInfoEnabled()) {
                StringBuffer logBuffer = new StringBuffer(64).append("Bean  ").append(repName).append(" started.");
                log.info(logBuffer.toString());
            }
        }
   
    }
    

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.user.UsersStore#getRepository(java.lang.String)
     */
    public UsersRepository getRepository(String name) {
        if (name == null || name.trim().equals("")) {
            name = defaultName;
        }

        UsersRepository response = objects.get(name);

        if ((response == null) && (log.isWarnEnabled())) {
            log.warn("No users repository called: " + name);
        }
        return response;
    }

    public void setDefaultRepository(String defaultName) {
        this.defaultName = defaultName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.user.UsersStore#getRepositoryNames()
     */
    public Iterator<String> getRepositoryNames() {
        return objects.keySet().iterator();
    }

}
