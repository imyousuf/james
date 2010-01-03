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

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;

/**
 * Loads instances of given types.
 */
public interface LoaderService {

    /**
     * Inject dependencies to the given object using jsr250. Before the injection is done set the Log and config 
     * to the object if the right LifeCycle methods are implement and Log / Config is not null
     * 
     * @param obj
     * @param logger
     * @param config
     */
    public void injectDependenciesWithLifecycle(Object obj, Log logger, HierarchicalConfiguration config);
    
    
    /**
     * Inject dependencies to the given object using jsr250
     * 
     * @param obj
     */
    public void injectDependencies(Object obj);
}
