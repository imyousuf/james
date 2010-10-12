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



package org.apache.james.impl.vut;

import java.util.Collection;

import javax.annotation.Resource;

import org.apache.james.api.vut.management.VirtualUserTableManagementException;
import org.apache.james.api.vut.management.VirtualUserTableManagementMBean;


/**
 * Management for VirtualUserTables
 * 
 */
public class VirtualUserTableManagementMBeanImpl implements VirtualUserTableManagementMBean {

    private org.apache.james.api.vut.management.VirtualUserTableManagement vut;    

    @Resource(name="virtualusertablemanagement")
    public void setVirtualUserTableManagement(org.apache.james.api.vut.management.VirtualUserTableManagement vut) {
        this.vut = vut;
    }
    

    public boolean addAddressMapping(String user, String domain, String address) throws  VirtualUserTableManagementException {
        return vut.addAddressMapping(user, domain, address);
    }

    public boolean addErrorMapping(String user, String domain, String error) throws VirtualUserTableManagementException {
        return vut.addErrorMapping(user, domain, error);  
    }

    
    public boolean addRegexMapping(String user, String domain, String regex) throws VirtualUserTableManagementException {
        return vut.addRegexMapping(user, domain, regex);  
    }


    public Collection<String> getUserDomainMappings(String user, String domain) throws VirtualUserTableManagementException {
        return vut.getUserDomainMappings(user, domain);
    }


    public boolean removeErrorMapping(String user, String domain, String error) throws VirtualUserTableManagementException {
        return vut.removeErrorMapping(user, domain, error);
    }

    public boolean removeRegexMapping(String user, String domain, String regex) throws VirtualUserTableManagementException {
        return vut.removeRegexMapping(user, domain, regex);
    }

   
    public boolean addMapping(String user, String domain, String mapping) throws VirtualUserTableManagementException {
        return vut.addMapping(user, domain, mapping);
    }


    public boolean removeMapping(String user, String domain, String mapping) throws VirtualUserTableManagementException {
        return vut.removeMapping(user, domain, mapping); 
    }


	public boolean removeAddressMapping(String user,String domain, String address)
			throws VirtualUserTableManagementException {
		return vut.removeAddressMapping(user, domain, address);
	       
	}

}
