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

import java.util.Collection;
import java.util.Map;

import javax.annotation.Resource;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.vut.api.VirtualUserTable;
import org.apache.james.vut.api.VirtualUserTableException;
import org.apache.james.vut.api.VirtualUserTableManagementMBean;

/**
 * Management for VirtualUserTables
 * 
 */
public class VirtualUserTableManagement extends StandardMBean implements VirtualUserTableManagementMBean {

    protected VirtualUserTableManagement() throws NotCompliantMBeanException {
        super(VirtualUserTableManagementMBean.class);
    }

    private VirtualUserTable vut;

    @Resource(name = "virtualusertable")
    public void setManageableVirtualUserTable(VirtualUserTable vut) {
        this.vut = vut;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.vut.api.VirtualUserTableManagementMBean#addRegexMapping
     * (java.lang.String, java.lang.String, java.lang.String)
     */
    public void addRegexMapping(String user, String domain, String regex) throws Exception {
        try {
            vut.addRegexMapping(user, domain, regex);
        } catch (VirtualUserTableException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.vut.api.VirtualUserTableManagementMBean#removeRegexMapping
     * (java.lang.String, java.lang.String, java.lang.String)
     */
    public void removeRegexMapping(String user, String domain, String regex) throws Exception {
        try {

            vut.removeRegexMapping(user, domain, regex);
        } catch (VirtualUserTableException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.vut.api.VirtualUserTableManagementMBean#addAddressMapping
     * (java.lang.String, java.lang.String, java.lang.String)
     */
    public void addAddressMapping(String user, String domain, String address) throws Exception {
        try {

            vut.addAddressMapping(user, domain, address);
        } catch (VirtualUserTableException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.vut.api.VirtualUserTableManagementMBean#removeAddressMapping
     * (java.lang.String, java.lang.String, java.lang.String)
     */
    public void removeAddressMapping(String user, String domain, String address) throws Exception {
        try {

            vut.removeAddressMapping(user, domain, address);
        } catch (VirtualUserTableException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.vut.api.VirtualUserTableManagementMBean#addErrorMapping
     * (java.lang.String, java.lang.String, java.lang.String)
     */
    public void addErrorMapping(String user, String domain, String error) throws Exception {
        try {

            vut.addErrorMapping(user, domain, error);
        } catch (VirtualUserTableException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.vut.api.VirtualUserTableManagementMBean#removeErrorMapping
     * (java.lang.String, java.lang.String, java.lang.String)
     */
    public void removeErrorMapping(String user, String domain, String error) throws Exception {
        try {

            vut.removeErrorMapping(user, domain, error);
        } catch (VirtualUserTableException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.vut.api.VirtualUserTableManagementMBean#
     * getUserDomainMappings(java.lang.String, java.lang.String)
     */
    public Collection<String> getUserDomainMappings(String user, String domain) throws Exception {
        try {

            return vut.getUserDomainMappings(user, domain);
        } catch (VirtualUserTableException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.vut.api.VirtualUserTableManagementMBean#addMapping(java
     * .lang.String, java.lang.String, java.lang.String)
     */
    public void addMapping(String user, String domain, String mapping) throws Exception {
        try {

            vut.addMapping(user, domain, mapping);
        } catch (VirtualUserTableException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.vut.api.VirtualUserTableManagementMBean#removeMapping
     * (java.lang.String, java.lang.String, java.lang.String)
     */
    public void removeMapping(String user, String domain, String mapping) throws Exception {
        try {

            vut.removeMapping(user, domain, mapping);
        } catch (VirtualUserTableException e) {
            throw new Exception(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.vut.api.VirtualUserTableManagementMBean#getAllMappings()
     */
    public Map<String, Collection<String>> getAllMappings() throws Exception {
        try {

            return vut.getAllMappings();
        } catch (VirtualUserTableException e) {
            throw new Exception(e.getMessage());
        }
    }

}
