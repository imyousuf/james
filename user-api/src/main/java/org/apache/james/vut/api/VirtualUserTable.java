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
package org.apache.james.vut.api;

import java.util.Collection;
import java.util.Map;

/**
 * Interface which should be implemented of classes which map recipients.
 */
public interface VirtualUserTable {

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
     * The wildcard used for alias domain mappings
     */
    public final static String WILDCARD = "*";

    /**
     * Return the mapped MailAddress for the given address. Return null if no 
     * matched mapping was found
     * 
     * @param user the MailAddress
     * @return the mapped mailAddress
     * @throws ErrorMappingException get thrown if an error mapping was found
     */
    public Collection<String> getMappings(String user, String domain) throws ErrorMappingException;
    
    
    /**
     * Add regex mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     */
    public boolean addRegexMapping(String user, String domain, String regex);
    
    /**
     * Remove regex mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param regex the regex.
     * @return true if successfully
     */
    public boolean removeRegexMapping(String user,String domain, String regex);
    
    /***
     * Add address mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param address 
     * @return true if successfully
     */
    public boolean addAddressMapping(String user, String domain, String address);
    
    /**
     * Remove address mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param address 
     * @return true if successfully
     */
    public boolean removeAddressMapping(String user,String domain, String address);
    
    /**
     * Add error mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param error the regex.
     * @return true if successfully
     */
    public boolean addErrorMapping(String user, String domain, String error);

    /**
     * Remove error mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param error
     * @return true if successfully
     * @throws ManageableVirtualUserTableException get thrown if an invalid argument was given
     */
    public boolean removeErrorMapping(String user,String domain, String error);
    
    /**
     * Return the explicit mapping stored for the given user and domain. Return null
     * if no mapping was found
     * 
     * @param user the username
     * @param domain the domain
     * @return the collection which holds the mappings. 
     */
    public Collection<String> getUserDomainMappings(String user, String domain);
    
    /**
     * Add mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param mapping the mapping
     * @return true if successfully
     */
    public boolean addMapping(String user, String domain, String mapping);
    
    /**
     * Remove mapping
     * 
     * @param user the username. Null if no username should be used
     * @param domain the domain. Null if no domain should be used
     * @param mapping the mapping
     * @return true if successfully
     */
    public boolean removeMapping(String user, String domain, String mapping);


    /**
     * Return a Map which holds all mappings. The key is the user@domain and the value is a Collection 
     * which holds all mappings
     * 
     * @return Map which holds all mappings
     */
    public Map<String,Collection<String>> getAllMappings();
    
    /**
     * Add aliasDomain mapping
     * 
     * @param aliasDomain the aliasdomain which should be mapped to the realDomain
     * @param realDomain the realDomain
     * @return true if successfully
     */
    public boolean addAliasDomainMapping(String aliasDomain, String realDomain);
    
    /**
     * Remove aliasDomain mapping
     * 
     * @param aliasDomain the aliasdomain which should be mapped to the realDomain
     * @param realDomain the realDomain
     * @return true if successfully
     */
    public boolean removeAliasDomainMapping(String aliasDomain, String realDomain);

    public class ErrorMappingException extends Exception {

        private static final long serialVersionUID = 2348752938798L;

        public ErrorMappingException(String string) {
            super(string);
        }

    }
}
