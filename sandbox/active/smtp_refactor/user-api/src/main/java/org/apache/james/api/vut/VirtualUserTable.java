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



package org.apache.james.api.vut;

import java.util.Collection;


/**
 * Interface which should be implemented of classes which map recipients
 *
 */
public interface VirtualUserTable {
    
    /**
     * The component role used by components implementing this service
     */
    public static final String ROLE = "org.apache.james.api.vut.VirtualUserTable";
    
    /**
     * The prefix which is used for error mappings
     */
    public static final String ERROR_PREFIX = "error:";
    
    /**
     * The prefix which is used for regex mappings
     */
    public static final String REGEX_PREFIX = "regex:";
    
    /**
     * The prefix which is used for alias domain mappings
     */
    public static final String ALIASDOMAIN_PREFIX = "domain:";
    
    /**
     * Return the mapped MailAddress for the given address. Return null if no 
     * matched mapping was found
     * 
     * @param user the MailAddress
     * @return the mapped mailAddress
     * @throws ErrorMappingException get thrown if an error mapping was found
     */
    public Collection getMappings(String user, String domain) throws ErrorMappingException;
}
