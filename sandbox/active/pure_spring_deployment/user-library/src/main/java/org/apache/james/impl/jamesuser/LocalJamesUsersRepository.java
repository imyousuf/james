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

import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.impl.user.LocalUsersRepository;

import java.util.Collection;

import javax.annotation.PostConstruct;

/**
 * This is a wrapper that provide access to the "LocalUsers" repository
 * but expect to find a JamesUsersRepository and return an object implementing
 * this extended interface
 */
public class LocalJamesUsersRepository extends LocalUsersRepository implements JamesUsersRepository{

    

    // REMOVE ME!!!
    @Override
    @PostConstruct
    public void init() throws Exception {
        super.init();
    }

    /**
     * @see org.apache.james.impl.jamesuser.JamesUsersRepository#setEnableAliases(boolean)
     */
    public void setEnableAliases(boolean enableAliases) {
        ((JamesUsersRepository) users).setEnableAliases(enableAliases);
    }

    /**
     * @see org.apache.james.impl.jamesuser.JamesUsersRepository#setEnableForwarding(boolean)
     */
    public void setEnableForwarding(boolean enableForwarding) {
        ((JamesUsersRepository) users).setEnableForwarding(enableForwarding);
    }

    /**
     * @see org.apache.james.impl.jamesuser.JamesUsersRepository#setIgnoreCase(boolean)
     */
    public void setIgnoreCase(boolean ignoreCase) {
        ((JamesUsersRepository) users).setIgnoreCase(ignoreCase);
    }

    /**
     * @see org.apache.james.api.vut.VirtualUserTable#getMappings(java.lang.String, java.lang.String)
     */
    public Collection<String> getMappings(String user, String domain) throws ErrorMappingException {
        return ((JamesUsersRepository) users).getMappings(user, domain);
    }

}
