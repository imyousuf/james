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



package org.apache.james.mailrepository.javamail;

import java.util.Collection;

/**
 * Used to map uids to keys and keys to uids 
 *
 */
public interface UidToKeyBidiMap {

    /**
     * Return true if an uid is stored under the given key 
     * 
     * @param key the key
     * @return true if an uid is stored under the given key
     */
    boolean containsKey(String key);

    /**
     * Store the given uid with the given key
     * 
     * @param key the key to store the given uid with 
     * @param uid the uid
     */
    void put(String key, long uid);

    /**
     * Remove the uid stored under the given key
     * 
     * @param key the key
     */
    void removeByKey(String key);

    /**
     * Return a String[] holding all stored keys
     * 
     * @return keys a String[] holding all keys
     */
    String[] getKeys();

    /**
     * Return the key for the given uid 
     * 
     * @param uid the uid 
     * @return key the key for the given uid
     */
    String getByUid(long uid);

    /**
     * 
     * @param keysBefore
     * @param keys
     */
    void retainAllListedAndAddedByKeys(String[] keysBefore, Collection keys);

    /**
     * Get uid for the given key
     * 
     * @param the key
     * @return uid the uid for the given key
     */
    long getByKey(String key);

}
