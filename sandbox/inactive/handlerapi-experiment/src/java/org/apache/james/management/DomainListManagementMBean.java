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



package org.apache.james.management;

import java.util.List;

import org.apache.james.management.DomainListManagementException;

public interface DomainListManagementMBean {

    /**
     * Add domain to the service
     * 
     * @phoenix:mx-operation
     * @phoenix:mx-description Add domain to the service
     *
     * 
     * @param domain domain to add
     * @return true if successfully
     * @throws DomainListManagementException 
     */
    public boolean addDomain(String domain) throws DomainListManagementException;
    
    /**
     * Remove domain from the service     
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Remove domain to the service  
     * 
     * @param domain domain to remove
     * @return true if succesfully
     * @throws DomainListManagementException 
     */
    public boolean removeDomain(String domain) throws DomainListManagementException;
    
    /**
     * Return List of domains which should be used as localdomains. Return null if no
     * domains were found
     * 
     * @phoenix:mx-operation
     * @phoenix:mx-description Return List of domains which should be used as localdomains. Return null if no
     *                         domains were found
     * 
     * @return domains
     */
    public List getDomains();
    
    /**
     * Return true if the domain exists in the service 
     * 
     * @phoenix:mx-operation
     * @phoenix:mx-description Return true if the domain exists in the service
     * 
     * @param domain the domain
     * @return true if the given domain exists in the service
     */
    public boolean containsDomain(String domain);
}
