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

import org.apache.james.api.user.InvalidMappingException;
import org.apache.james.api.vut.VirtualUserTable;

public interface VirtualUserTableManagement extends VirtualUserTable{
    
    /**
     * The component role used by components implementing this service
     */
    public static final String ROLE = "org.apache.james.services.VirtualUserTableManagement";
    
    /**
     * Add regex mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean addRegexMapping(String user, String domain, String regex) throws InvalidMappingException;
    
    /**
     * Remove regex mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean removeRegexMapping(String user,String domain, String regex) throws InvalidMappingException;
    
    /***
     * Add address mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param address 
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean addAddressMapping(String user, String domain, String address) throws InvalidMappingException;
    
    /**
     * Remove address mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param address 
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean removeAddressMapping(String user,String domain, String address) throws InvalidMappingException;
    
    /**
     * Add error mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param error the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean addErrorMapping(String user, String domain, String error) throws InvalidMappingException;

    /**
     * Remove error mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param error
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean removeErrorMapping(String user,String domain, String error) throws InvalidMappingException;
    
    /**
     * Return the explicit mapping stored for the given user and domain. Return null
     * if no mapping was found
     * 
     * @param user the username
     * @param domain the domain
     * @return the collection which holds the mappings. 
     * @throws InvalidMappingException  get thrown if an invalid use or domain was given
     */
    public Collection getUserDomainMappings(String user, String domain) throws InvalidMappingException;
    
    /**
     * Add mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param mapping the mapping
     * @return true if successfully
     * @throws InvalidMappingException
     */
    public boolean addMapping(String user, String domain, String mapping) throws InvalidMappingException;
    
    /**
     * Remove mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param mapping the mapping
     * @return true if successfully
     * @throws InvalidMappingException
     */
    public boolean removeMapping(String user, String domain, String mapping) throws InvalidMappingException;


    /**
     * Return a Map which holds all mappings. The key is the user@domain and the value is a Collection 
     * which holds all mappings
     * 
     * @return Map which holds all mappings
     */
    public Map getAllMappings();
    
    /**
     * Add aliasDomain mapping
     * 
     * @param aliasDomain the aliasdomain which should be mapped to the realDomain
     * @param realDomain the realDomain
     * @return true if successfilly
     * @throws InvalidMappingException
     */
    public boolean addAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException;
    
    /**
     * Remove aliasDomain mapping
     * 
     * @param aliasDomain the aliasdomain which should be mapped to the realDomain
     * @param realDomain the realDomain
     * @return true if successfilly
     * @throws InvalidMappingException
     */
    public boolean removeAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException;
}
