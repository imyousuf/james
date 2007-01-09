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

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;

import java.util.Iterator;

/**
 * Provides a registry of user repositories.
 *
 */
public class AvalonUsersStore
    extends AbstractAvalonStore
    implements UsersStore {

    /** 
     * Get the repository, if any, whose name corresponds to
     * the argument parameter
     *
     * @param name the name of the desired repository
     *
     * @return the UsersRepository corresponding to the name parameter
     */
    public UsersRepository getRepository(String name) {
        UsersRepository response = (UsersRepository) getObject(name);
        if ((response == null) && (getLogger().isWarnEnabled())) {
            getLogger().warn("No users repository called: " + name);
        }
        return response;
    }

    /** 
     * Yield an <code>Iterator</code> over the set of repository
     * names managed internally by this store.
     *
     * @return an Iterator over the set of repository names
     *         for this store
     */
    public Iterator getRepositoryNames() {
        return getObjectNames();
    }
    
    /**
     * @see org.apache.james.core.AbstractAvalonStore#getClassInstance(java.lang.ClassLoader, java.lang.String)
     */
    public Object getClassInstance(ClassLoader loader, String repClass) throws Exception {
        return  (UsersRepository) loader.loadClass(repClass).newInstance();
    }

    /**
     * @see org.apache.james.core.AbstractAvalonStore#getConfigurations(org.apache.avalon.framework.configuration.Configuration)
     */
    public Configuration[] getConfigurations(Configuration config) {
        return configuration.getChildren("repository");
    }
    
    /**
     * @see org.apache.james.core.AbstractAvalonStore#getStoreName()
     */
    public String getStoreName() {
        return "AvolonUsersStore";
    }
}
