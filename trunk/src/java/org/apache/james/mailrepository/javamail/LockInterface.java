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

/**
 * Interface for org.apache.james.util.Lock functionality to be able to replace
 * implementation or using Mock-objects at tests
 * 
 * @see org.apache.james.util.Lock
 */
public interface LockInterface {

    /**
     * Check to see if the object is locked
     *
     * @param key the Object on which to check the lock
     * @return true if the object is locked, false otherwise
     */
    public boolean isLocked(final Object key);

    /**
     * Lock on a given object.
     *
     * @param key the Object on which to lock
     * @return true if the locking was successful, false otherwise
     */
    public boolean lock(final Object key);

    /**
     * Release the lock on a given object.
     *
     * @param key the Object on which the lock is held
     * @return true if the unlocking was successful, false otherwise
     */
    public boolean unlock(final Object key);

}
