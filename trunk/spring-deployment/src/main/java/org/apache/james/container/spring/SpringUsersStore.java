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
package org.apache.james.container.spring;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * UsersStore implementation which will parse the configuration file for users-store and add every configured repository
 * to the Spring BeanFactory.
 * @author norman
 *
 */
public class SpringUsersStore extends AbstractStore implements UsersStore {

    private String defaultName;


    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.user.UsersStore#getRepository(java.lang.String)
     */
    public UsersRepository getRepository(String name) {
        if (name == null || name.trim().equals("")) {
            name = defaultName;
        }

        UsersRepository response = null;

        if (objects.contains(name)) {
            try {
                response = (UsersRepository) context.getBean(name, UsersRepository.class);
            } catch (NoSuchBeanDefinitionException e) {
                // Just catch the exception
            }
        }
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
        return objects.iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<HierarchicalConfiguration> getSubConfigurations(HierarchicalConfiguration rootConf) {
        return rootConf.configurationsAt("repository");
    }

}
