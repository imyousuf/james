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

import java.util.List;

/**
 * This interface provide should be implementated by services which
 * offer domains for which email will accepted
 *
 */
public interface DomainList {

    /**
     * The component role used by components implementing this service
     */
    public final static String ROLE ="org.apache.james.services.DomainList";
    
    /**
     * Return List of domains which should be used as localdomains. Return null if no
     * domains were found
     * 
     * @return domains
     */
    public List getDomains();
    
    /**
     * Return true if the domain exists in the service 
     * 
     * @param domain the domain
     * @return true if the given domain exists in the service
     */
    public boolean containsDomain(String domain);

    /**
     * Set to true to autodetect the hostname of the host on which
     * james is runnin, and add this to the domain service 
     * Default is true
     * 
     * @param autodetect set to false for disable
     */
    public void setAutoDetect(boolean autodetect);
    
    
    /**
     * Set to true to lookup the ipaddresses for each given domain
     * and add these to the domain service 
     * Default is true
     * 
     * @param autodetectIP set to false for disable
     */
    public void setAutoDetectIP(boolean autodetectIP);
    
}
