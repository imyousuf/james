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

package org.apache.james.impl.jamesuser;

import java.util.Collection;

import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.impl.user.AvalonLocalUsersRepository;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.Guice;

public class AvalonLocalJamesUsersRepository extends AvalonLocalUsersRepository implements JamesUsersRepository{
    
    public void initialize() throws Exception {
        repos = Guice.createInjector(new Jsr250Module(), new LocalUsersRepositoryModule()).getInstance(LocalJamesUsersRepository.class);
    }
    
    public void setEnableAliases(boolean enableAliases) {
        ((LocalJamesUsersRepository) repos).setEnableAliases(enableAliases);
    }

    public void setEnableForwarding(boolean enableForwarding) {
        ((LocalJamesUsersRepository) repos).setEnableForwarding(enableForwarding);
    }

    public void setIgnoreCase(boolean ignoreCase) {
        ((LocalJamesUsersRepository) repos).setIgnoreCase(ignoreCase);
    }

    public Collection<String> getMappings(String user, String domain)
            throws ErrorMappingException {
        return ((LocalJamesUsersRepository) repos).getMappings(user, domain);
    }

}
