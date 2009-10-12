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



package org.apache.james.api.vut.management;

import java.util.Collection;
import java.util.Map;


public interface VirtualUserTableManagementService {
    
    /**
     * The component role used by components implementing this service
     */
    public static final String ROLE = "org.apache.james.api.vut.management.VirtualUserTableManagementService";

    /**
     * Add regex mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws VirtualUserTableManagementException get thrown if an invalid argument was given
     */
    public boolean addRegexMapping(String virtualUserTable, String user, String domain, String regex) throws VirtualUserTableManagementException;
    
    /**
     * Remove regex mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws VirtualUserTableManagementException get thrown if an invalid argument was given
     */
    public boolean removeRegexMapping(String virtualUserTable, String user,String domain, String regex) throws VirtualUserTableManagementException;
    
    /***
     * Add address mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param address 
     * @return true if successfully
     * @throws VirtualUserTableManagementException get thrown if an invalid argument was given
     */
    public boolean addAddressMapping(String virtualUserTable, String user, String domain, String address) throws VirtualUserTableManagementException;
    
    /**
     * Remove address mapping
     *
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param address 
     * @return true if successfully
     * @throws VirtualUserTableManagementException get thrown if an invalid argument was given
     */
    public boolean removeAddressMapping(String virtualUserTable, String user,String domain, String address) throws VirtualUserTableManagementException;
    
    /**
     * Add error mapping
     *
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param error
     * @return true if successfully
     * @throws VirtualUserTableManagementException get thrown if an invalid argument was given
     */
    public boolean addErrorMapping(String virtualUserTable, String user, String domain, String error) throws VirtualUserTableManagementException;

    /**
     * Remove error mapping
     *
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param error
     * @return true if successfully
     * @throws VirtualUserTableManagementException get thrown if an invalid argument was given
     */
    public boolean removeErrorMapping(String virtualUserTable, String user,String domain, String error) throws VirtualUserTableManagementException;
    
    /**
     * Return the explicit mapping stored for the given user and domain. Return null
     * if no mapping was found
     *
     * @param virtualUserTable The virtualUserTable     
     * @param user the username
     * @param domain the domain
     * @return the collection which holds the mappings. 
     * @throws VirtualUserTableManagementException  get thrown if an invalid use or domain was given
     */
    public Collection getUserDomainMappings(String virtualUserTable, String user, String domain) throws VirtualUserTableManagementException;
    
    /**
    * Try to identify the right method based on the prefix of the mapping and add it.
    *
    * @param virtualUserTable The virtualUserTable 
    * @param user the username. Null if no username should be used
    * @param domain the domain. Null if no domain should be used
    * @param mapping the mapping.
    * @return true if successfully
    * @throws VirtualUserTableManagementException get thrown if an invalid argument was given
    */
    public boolean addMapping(String virtualUserTable, String user, String domain, String mapping) throws VirtualUserTableManagementException;
    
    /**
     * Try to identify the right method based on the prefix of the mapping and remove it.
     *
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param mapping the mapping.
     * @return true if successfully
     * @throws VirtualUserTableManagementException get thrown if an invalid argument was given
     */
    public boolean removeMapping(String virtualUserTable, String user, String domain, String mapping) throws VirtualUserTableManagementException;

    /**
     * Return a Map which holds all mappings
     * 
     * @param virtualUserTable The virtualUserTable 
     * @return Map which holds all mappings
     * @throws VirtualUserTableManagementException 
     */
    public Map getAllMappings(String virtualUserTable) throws VirtualUserTableManagementException;
    
    /**
     * Add aliasDomain mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param aliasDomain the aliasdomain which should be mapped to the realDomain
     * @param realDomain the realDomain
     * @return true if successfilly
     * @throws InvalidMappingException
     */
    public boolean addAliasDomainMapping(String virtualUserTable, String aliasDomain, String realDomain) throws VirtualUserTableManagementException;
    
    /**
     * Remove aliasDomain mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param aliasDomain the aliasdomain which should be mapped to the realDomain
     * @param realDomain the realDomain
     * @return true if successfilly
     * @throws InvalidMappingException
     */
    public boolean removeAliasDomainMapping(String virtualUserTable, String aliasDomain, String realDomain) throws VirtualUserTableManagementException;
}
