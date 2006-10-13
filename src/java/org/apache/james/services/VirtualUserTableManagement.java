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

import org.apache.james.vut.InvalidMappingException;

public interface VirtualUserTableManagement extends VirtualUserTable{
    
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
    public boolean removeRegexMapping(String user,String domain, String regex);
    
    /***
     * Add address mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean addAddressMapping(String user, String domain, String address) throws InvalidMappingException;
    
    /**
     * Remove address mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean removeAddressMapping(String user,String domain, String address);
    
    /**
     * Add error mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean addErrorMapping(String user, String domain, String error) throws InvalidMappingException;

    /**
     * Remove error mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     * @throws InvalidMappingException get thrown if an invalid argument was given
     */
    public boolean removeErrorMapping(String user,String domain, String error);
}
