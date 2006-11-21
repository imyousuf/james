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

import java.util.Collection;

import org.apache.james.management.VirtualUserTableManagementException;
import org.apache.james.vut.InvalidMappingException;

/**
 * Expose virtualusertable management functionality through JMX.
 * 
 * @phoenix:mx-topic name="VirtualUserTableAdministration"
 */
public interface VirtualUserTableManagementMBean {
    
    /**
     * Add regex mapping
     * @phoenix:mx-operation
     * @phoenix:mx-description Add regex mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean addRegexMapping(String virtualUserTable, String user, String domain, String regex) throws VirtualUserTableManagementException;
    
    /**
     * Remove regex mapping
     * @phoenix:mx-operation
     * @phoenix:mx-description Remove regex mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean removeRegexMapping(String virtualUserTable, String user,String domain, String regex) throws VirtualUserTableManagementException;
    
    /***
     * Add address mapping
     * @phoenix:mx-operation
     * @phoenix:mx-description Add address mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean addAddressMapping(String virtualUserTable, String user, String domain, String address) throws VirtualUserTableManagementException;
    
    /**
     * Remove address mapping
     * @phoenix:mx-operation
     * @phoenix:mx-description Remove address mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean removeAddressMapping(String virtualUserTable, String user,String domain, String address) throws VirtualUserTableManagementException;
    
    /**
     * Add error mapping
     * @phoenix:mx-operation
     * @phoenix:mx-description Add error mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean addErrorMapping(String virtualUserTable, String user, String domain, String error) throws VirtualUserTableManagementException;

    /**
     * Remove error mapping
     * @phoenix:mx-operation
     * @phoenix:mx-description Remove error mapping
     * 
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean removeErrorMapping(String virtualUserTable, String user,String domain, String error) throws VirtualUserTableManagementException;
    
    /**
     * Return the explicit mapping stored for the given user and domain. Return null
     * if no mapping was found
     * @phoenix:mx-operation
     * @phoenix:mx-description Return the explicit mapping stored for the given user and domain. Return null
     *                         if no mapping was found
     *
     * @param virtualUserTable The virtualUserTable     
     * @param user the username
     * @param domain the domain
     * @return the collection which holds the mappings. 
     * @throws InvalidMappingException  get thrown if an invalid use or domain was given
     */
    public Collection getUserDomainMappings(String virtualUserTable, String user, String domain) throws VirtualUserTableManagementException;
    
    /**
    * Try to identify the right method based on the prefix of the mapping and add it.
    * @phoenix:mx-operation
    * @phoenix:mx-description Try to identify the right method based on the prefix of the mapping and add it
    * 
    * @param virtualUserTable The virtualUserTable 
    * @param user the username. Null if no username should be used
    * @param domain the domain. Null if no domain should be used
    * @param mapping the mapping.
    * @return true if successfully
    * @throws InvalidMappingException get thrown if an invalid argument was given
    */
    public boolean addMapping(String virtualUserTable, String user, String domain, String mapping) throws VirtualUserTableManagementException;
    
    /**
     * Try to identify the right method based on the prefix of the mapping and remove it.
     * @phoenix:mx-operation
     * @phoenix:mx-description Try to identify the right method based on the prefix of the mapping and remove it
     *
     * @param virtualUserTable The virtualUserTable 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param mapping the mapping.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean removeMapping(String virtualUserTable, String user, String domain, String mapping) throws VirtualUserTableManagementException;
}
