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
import java.util.Map;

import javax.annotation.Resource;

import org.apache.james.vut.api.ManageableVirtualUserTableException;
import org.apache.james.vut.api.management.VirtualUserTableManagementMBean;


/**
 * Management for VirtualUserTables
 * 
 */
public class VirtualUserTableManagement implements VirtualUserTableManagementMBean {

    private org.apache.james.vut.api.ManageableVirtualUserTable vut;    

    @Resource(name="manageablevirtualusertable")
    public void setManageableVirtualUserTable(org.apache.james.vut.api.ManageableVirtualUserTable vut) {
        this.vut = vut;
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.management.VirtualUserTableManagementMBean#addAddressMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addAddressMapping(String user, String domain, String address) {
        try {
            return vut.addAddressMapping(user, domain, address);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.management.VirtualUserTableManagementMBean#addErrorMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addErrorMapping(String user, String domain, String error) {
        try {
            return vut.addErrorMapping(user, domain, error); 
        } catch (ManageableVirtualUserTableException e){
            return false;
        }
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.management.VirtualUserTableManagementMBean#addRegexMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addRegexMapping(String user, String domain, String regex) {
        try {
            return vut.addRegexMapping(user, domain, regex);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }  
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.management.VirtualUserTableManagementMBean#getUserDomainMappings(java.lang.String, java.lang.String)
     */
    public Collection<String> getUserDomainMappings(String user, String domain) {
        try {
            return vut.getUserDomainMappings(user, domain);
        } catch (ManageableVirtualUserTableException e) {
            return new ArrayList<String>();
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.management.VirtualUserTableManagementMBean#removeErrorMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeErrorMapping(String user, String domain, String error) {
        try {
            return vut.removeErrorMapping(user, domain, error);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.management.VirtualUserTableManagementMBean#removeRegexMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeRegexMapping(String user, String domain, String regex) {
        try {
            return vut.removeRegexMapping(user, domain, regex);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }
    }

   
    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.management.VirtualUserTableManagementMBean#addMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addMapping(String user, String domain, String mapping) {
        try {
            return vut.addMapping(user, domain, mapping);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.management.VirtualUserTableManagementMBean#removeMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeMapping(String user, String domain, String mapping) {
        try {
            return vut.removeMapping(user, domain, mapping);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        } 
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.management.VirtualUserTableManagementMBean#removeAddressMapping(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeAddressMapping(String user, String domain, String address) {
        try {
            return vut.removeAddressMapping(user, domain, address);
        } catch (ManageableVirtualUserTableException e) {
            return false;
        }

    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.management.VirtualUserTableManagementMBean#getAllMappings()
     */
    public Map<String, Collection<String>> getAllMappings() {
        return vut.getAllMappings();
      
    }

}
