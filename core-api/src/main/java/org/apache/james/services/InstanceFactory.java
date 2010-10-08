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

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;

/**
 * Loads instances of given types.
 */
public interface InstanceFactory {

    /**
     * Create an instance of the given class.
     * Instances should - where appropriate - have dependencies injected.
     * @param <T> 
     * @param type may be interface or concrete, not null
     * @return an instance of the type
     */
    public <T> T newInstance(Class<T> clazz) throws InstanceException;
    
    public <T> T newInstance(Class<T> clazz, Log log, HierarchicalConfiguration config) throws InstanceException;

    
    @SuppressWarnings("serial")
    public class InstanceException extends Exception {
        public InstanceException(String msg, Throwable t) {
            super(msg,t);
        }
        
        public InstanceException(String msg) {
            super(msg);
        }
    }

}
