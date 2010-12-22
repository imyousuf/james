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
package org.apache.james.mailetcontainer.api;

import java.util.List;


/**
 * {@link MailProcessor} which delegate the work to child {@link MailProcessor}
 * implementations
 */
public interface CompositeMailProcessor extends MailProcessor{

    /**
     * @return names of all configured processor
     */
    public String[] getProcessorNames();

    /**
     * @return access the child processor
     */
    public MailProcessor getProcessor(String name);

    /**
     * Add a {@link CompositeMailProcessorListener} which will get triggered after a child {@link MailProcessor} finish
     * processing
     * 
     * @param listener
     */
    public void addListener(CompositeMailProcessorListener listener);
    
    /**
     * Remove a {@link CompositeMailProcessorListener}
     * 
     * @param listener
     */
    public void removeListener(CompositeMailProcessorListener listener);
    
    /**
     * Return a unmodifiable {@link List} of {@link CompositeMailProcessorListener} which are registered 
     * 
     * @return listeners
     */
    public List<CompositeMailProcessorListener> getListeners();
}
