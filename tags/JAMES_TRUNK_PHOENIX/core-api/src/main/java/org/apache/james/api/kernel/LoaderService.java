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

package org.apache.james.api.kernel;

/**
 * Loads instances of given types.
 */
public interface LoaderService {
    
    /**
     * Loads an instance of the given class.
     * The load may elect to return a new instance
     * or reuse an existing one, as appropriate for the type.
     * Instances should - where appropriate - have dependencies injected.
     * @param <T> 
     * @param type may be interface or concrete, not null
     * @return an instance of the type
     */
    public <T>T load(Class<T> type);
}
