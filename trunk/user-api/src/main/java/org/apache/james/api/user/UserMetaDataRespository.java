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

package org.apache.james.api.user;

import java.io.Serializable;

/**
 * <p>Contains flexible meta-data related to users.
 * Values should be serializable to allow easy remote transport
 * and persistence. Keys are strings.
 * </p><p>
 * <strong>Note</strong> conventionally keys should be URIs
 * and so naturally namespaced. In particular, all keys starting with
 * <code>http://james.apache.org/</code> are reserved for 
 * use by JAMES.
 * </p>
 */
public interface UserMetaDataRespository {
    
    /**
     * Gets the attribute for the given key.
     * @param username the name of the user, not null
     * @param key conventionally an URI, not null
     * @return value, or null if the keyed attribute has 
     * no associated value.
     */
    public Serializable getAttribute(String username, String key) throws UserRepositoryException;
    
    /**
     * Sets the attribute keyed to the given value.
     * @param username the name of the user which meta-data is to be set, not null
     * @param value <code>Serializable</code> value, possibly null
     * @param key conventionally an URI, not null
     */
    public void setAttribute(String username, Serializable value, String key) throws UserRepositoryException;
    
    /**
     * Clears all attributes for the given user.
     * @param username the name of the user who meta data is to be cleared, not null
     * @throws UserRepositoryException
     */
    public void clear(String username) throws UserRepositoryException;
}
