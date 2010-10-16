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

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Resource;

import org.apache.james.api.vut.ManageableVirtualUserTableException;
import org.apache.james.api.vut.management.VirtualUserTableManagementMBean;


/**
 * Management for VirtualUserTables
 * 
 */
public class VirtualUserTableManagement implements VirtualUserTableManagementMBean {

    private org.apache.james.api.vut.ManageableVirtualUserTable vut;    

    @Resource(name="manageablevirtualusertable")
    public void setManageableVirtualUserTable(org.apache.james.api.vut.ManageableVirtualUserTable vut) {
        this.vut = vut;
    }
    

    public boolean addAddressMapping(String user, String domain, String address) {
        try {
            return vut.addAddressMapping(user, domain, address);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }
    }

    public boolean addErrorMapping(String user, String domain, String error) {
        try {
            return vut.addErrorMapping(user, domain, error); 
        } catch (ManageableVirtualUserTableException e){
            return false;
        }
    }

    
    public boolean addRegexMapping(String user, String domain, String regex) {
        try {
            return vut.addRegexMapping(user, domain, regex);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }  
    }


    public Collection<String> getUserDomainMappings(String user, String domain) {
        try {
            return vut.getUserDomainMappings(user, domain);
        } catch (ManageableVirtualUserTableException e) {
            return new ArrayList<String>();
        }
    }


    public boolean removeErrorMapping(String user, String domain, String error) {
        try {
            return vut.removeErrorMapping(user, domain, error);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }
    }

    public boolean removeRegexMapping(String user, String domain, String regex) {
        try {
            return vut.removeRegexMapping(user, domain, regex);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }
    }

   
    public boolean addMapping(String user, String domain, String mapping) {
        try {
            return vut.addMapping(user, domain, mapping);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }
    }


    public boolean removeMapping(String user, String domain, String mapping) {
        try {
            return vut.removeMapping(user, domain, mapping);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        } 
    }


    public boolean removeAddressMapping(String user, String domain, String address) {
        try {
            return vut.removeAddressMapping(user, domain, address);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }

    }

}
