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

package org.apache.james.user.impl.file;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.api.user.UserMetaDataRespository;
import org.apache.james.api.user.UserRepositoryException;

public class FileUserMetaDataService implements UserMetaDataRespository {

    private UserMetaDataRespository repository;
    private HierarchicalConfiguration configuration;
    
    @Resource(name="org.apache.commons.configuration.Configuration")
    public void setConfiguration(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * @see org.apache.james.api.user.UserMetaDataRespository#clear(java.lang.String)
     */
    public void clear(String username) throws UserRepositoryException {
        repository.clear(username);
    }

    /**
     * @see org.apache.james.api.user.UserMetaDataRespository#getAttribute(java.lang.String, java.lang.String)
     */
    public Serializable getAttribute(String username, String key)
            throws UserRepositoryException {
        return repository.getAttribute(username, key);
    }

    /**
     * @see org.apache.james.api.user.UserMetaDataRespository#setAttribute(java.lang.String, java.io.Serializable, java.lang.String)
     */
    public void setAttribute(String username, Serializable value, String key)
            throws UserRepositoryException {
        repository.setAttribute(username, value, key);
    }

    @PostConstruct
    public void init() throws Exception {
        String baseDirectory = configuration.getString("[@baseDir]");

        repository = new FileUserMetaDataRepository(baseDirectory);
    }

}
