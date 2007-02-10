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

package org.apache.james.mailboxmanager.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class KeyBidiMapImpl  implements KeyBidiMap {

        private Map externalToInternal;

        private Map internalToExternal;

        public KeyBidiMapImpl() {
            externalToInternal = new HashMap();
            internalToExternal = new HashMap();
        }

        public synchronized String[] getExternalKeys() {
            final ArrayList al = new ArrayList(externalToInternal.keySet());
            final String[] keys = (String[]) al.toArray(new String[0]);
            return keys;
        }

        public synchronized void retainAllListedAndAddedByExternalKeys(String[] externalKeysBefore, Collection externalKeys) {
            Collection added = new HashSet(externalToInternal.keySet());
            added.removeAll(Arrays.asList(externalKeysBefore));
            Collection retain = new HashSet(externalKeys);
            retain.addAll(added);
            externalToInternal.keySet().retainAll(retain);
            internalToExternal.keySet().retainAll(externalToInternal.values());
        }

        public synchronized void removeByExternalKey(String externalKey) {
            String internalKey = getByExternalKey(externalKey);
            internalToExternal.remove(internalKey);
            externalToInternal.remove(externalKey);
        }

        public synchronized String getByExternalKey(String externalKey) {
            return (String) externalToInternal.get(externalKey);
        }

        public synchronized String getByInternalKey(String internalKey) { 
            return (String) internalToExternal.get(internalKey);
        }

        public synchronized boolean containsExternalKey(String externalKey) {
            return externalToInternal.containsKey(externalKey);
        }

        public synchronized void put(String externalKey, String internalKey) {
            externalToInternal.put(externalKey, internalKey);
            internalToExternal.put(internalKey, externalKey);
        }


}
