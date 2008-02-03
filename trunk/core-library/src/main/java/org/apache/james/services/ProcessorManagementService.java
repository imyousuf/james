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

import org.apache.james.management.ProcessorManagementMBean;

public interface ProcessorManagementService extends ProcessorManagementMBean {

    public static final String ROLE = "org.apache.james.services.ProcessorManagementService";
    
    /**
     * retrieves the list of all mailets for one processor
     * @param processorName
     * @return array of names
     */
    String[] getMailetNames(String processorName);

    
    /**
     * retrieves the list of all matchers for one processor
     * @param processorName
     * @return array of names
     */
    String[] getMatcherNames(String processorName);

    /**
     * retrieves the list of parameters belonging to the specified matcher
     * @param processorName
     * @param matcherIndex
     * @return array of Strings, each String an assembled parameter key/value pair
     */
    String[] getMatcherParameters(String processorName, int matcherIndex);

    /**
     * retrieves the list of parameters belonging to the specified mailet
     * @param processorName
     * @param mailetIndex
     * @return array of Strings, each String an assembled parameter key/value pair
     */
    String[] getMailetParameters(String processorName, int mailetIndex);
}
