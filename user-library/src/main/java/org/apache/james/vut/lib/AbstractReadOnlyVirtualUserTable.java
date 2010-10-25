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
package org.apache.james.vut.lib;


import org.apache.james.vut.api.VirtualUserTable;

/**
 * Abstract base class which can be used by {@link VirtualUserTable} implementations which are read-only
 * 
 *
 */
public abstract class AbstractReadOnlyVirtualUserTable implements VirtualUserTable{

    /**
     * Do nothing and return false
     */
    public boolean addAddressMapping(String user, String domain, String address) {
        return false;
    }

    /**
     * Do nothing and return false
     */
    public boolean addAliasDomainMapping(String aliasDomain, String realDomain) {
        return false;
    }

    /**
     * Do nothing and return false
     */
    public boolean addErrorMapping(String user, String domain, String error) {
        return false;
    }

    /**
     * Do nothing and return false
     */
    public boolean addMapping(String user, String domain, String mapping) {
        return false;
    }

    /**
     * Do nothing and return false
     */
    public boolean addRegexMapping(String user, String domain, String regex) {
        return false;
    }

    /**
     * Do nothing and return false
     */
    public boolean removeAddressMapping(String user, String domain, String address) {
        return false;
    }

    /**
     * Do nothing and return false
     */
    public boolean removeAliasDomainMapping(String aliasDomain, String realDomain) {
        return false;
    }

    /**
     * Do nothing and return false
     */
    public boolean removeErrorMapping(String user, String domain, String error) {
        return false;
    }

    /**
     * Do nothing and return false
     */
    public boolean removeMapping(String user, String domain, String mapping) {
        return false;
    }

    /**
     * Do nothing and return false
     */
    public boolean removeRegexMapping(String user, String domain, String regex) {
        return false;
    }

}
