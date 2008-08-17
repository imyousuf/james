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

package org.apache.james.phoenix.user;

import java.io.Serializable;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.api.user.UserMetaDataRespository;
import org.apache.james.api.user.UserRepositoryException;
import org.apache.james.user.impl.file.FileUserMetaDataRepository;

public class FileUserMetaDataService extends AbstractLogEnabled 
        implements UserMetaDataRespository, Configurable, Initializable {

    private UserMetaDataRespository repository;
    private String baseDirectory;
    
    public void clear(String username) throws UserRepositoryException {
        repository.clear(username);
    }

    public Serializable getAttribute(String username, String key)
            throws UserRepositoryException {
        return repository.getAttribute(username, key);
    }

    public void setAttribute(String username, Serializable value, String key)
            throws UserRepositoryException {
        repository.setAttribute(username, value, key);
    }

    public void configure(final Configuration configuration) throws ConfigurationException {
        baseDirectory = configuration.getAttribute("baseDir");
    }

    public void initialize() throws Exception {
        repository = new FileUserMetaDataRepository(baseDirectory);
    }

}
