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



package org.apache.james.api.domainlist;


/**
 * Services which allow to manage the DomainList should implement this interface
 */
public interface ManageableDomainList extends DomainList {

    public final static String ROLE = "org.apache.james.api.domainlist.ManageableDomainList";
    
    /**
     * Add domain to the service
     * 
     * @param domain domain to add
     * @return true if successfully
     */
    public boolean addDomain(String domain);
    
    /**
     * Remove domain from the service
     *  
     * @param domain domain to remove
     * @return true if succesfully
     */
    public boolean removeDomain(String domain);
}
