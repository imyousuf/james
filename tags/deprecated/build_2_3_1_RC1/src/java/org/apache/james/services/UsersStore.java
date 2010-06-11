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

package org.apache.james.services;

import java.util.Iterator;

/**
 * Interface for Phoenix blocks to access a store of Users. A UserStore
 * contains one or more UserRepositories. Multiple UserRepositories may or may
 * not have overlapping membership. 
 *
 * @version 1.0.0, 24/04/1999
 */
public interface UsersStore 
{
    /**
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.services.UsersStore";

    /** 
     * Get the repository, if any, whose name corresponds to
     * the argument parameter
     *
     * @param name the name of the desired repository
     *
     * @return the UsersRepository corresponding to the name parameter
     */
    UsersRepository getRepository( String name );

    /** 
     * Yield an <code>Iterator</code> over the set of repository
     * names managed internally by this store.
     *
     * @return an Iterator over the set of repository names
     *         for this store
     */
    Iterator getRepositoryNames();
}
