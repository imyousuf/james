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

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.core.DefaultVirtualUserTable;
import org.apache.james.services.VirtualUserTableManagementService;
import org.apache.james.services.VirtualUserTableStore;
import org.apache.james.vut.InvalidMappingException;


/**
 * Management for VirtualUserTables
 * 
 */
public class VirtualUserTableManagement implements Serviceable, VirtualUserTableManagementService, VirtualUserTableManagmentMBean {

    VirtualUserTableStore store;
    org.apache.james.services.VirtualUserTableManagement defaultVUT;    

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        setVirtualUserTableStore((VirtualUserTableStore) arg0.lookup(VirtualUserTableStore.ROLE));
        setDefaultVirtualUserTable((org.apache.james.services.VirtualUserTableManagement) arg0.lookup(DefaultVirtualUserTable.ROLE));
    }

    public void setVirtualUserTableStore(VirtualUserTableStore store) {
        this.store = store;
    }
    
    public void setDefaultVirtualUserTable(org.apache.james.services.VirtualUserTableManagement defaultVUT) {
        this.defaultVUT = defaultVUT;
    }
    
    /**
     * Return a VirtualUserTableManagement with the given tablename
     * 
     * @param tableName tableName if null is given the DefaultVirtualUserTable get returned
     * @return VirtualUserTableManagement object
     * @throws VirtualUserTableManagementException if no VirtualUserTable with the given name exists
     */
    private org.apache.james.services.VirtualUserTableManagement getTable(String tableName) throws VirtualUserTableManagementException {     
        // if the tableName was null return the DefaultVirtualUserTable
        if (tableName == null) {
            return defaultVUT;
        } else {
            org.apache.james.services.VirtualUserTableManagement vut = (org.apache.james.services.VirtualUserTableManagement) store.getTable(tableName);
    
            // Check if a table with the given name exists, if not throw an Exception
            if (vut == null) {
                throw new VirtualUserTableManagementException("No VirtualUserTable with such name: " + tableName);
            } else {
                return vut;
            }
        }
    }
    
    /**
     * @see org.apache.james.services.VirtualUserTableManagementService#addAddressMapping(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addAddressMapping(String virtualUserTable, String user, String domain, String address) throws  VirtualUserTableManagementException {
        try {
            return getTable(virtualUserTable).addAddressMapping(user, domain, address);
        } catch (InvalidMappingException e) {
            throw new VirtualUserTableManagementException(e);
        }
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagementService#addErrorMapping(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addErrorMapping(String virtualUserTable, String user, String domain, String error) throws VirtualUserTableManagementException {
        try {
            return getTable(virtualUserTable).addErrorMapping(user, domain, error);
        } catch (InvalidMappingException e) {
            throw new VirtualUserTableManagementException(e);
        }
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagementService#addRegexMapping(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addRegexMapping(String virtualUserTable, String user, String domain, String regex) throws VirtualUserTableManagementException {
        try {
            return getTable(virtualUserTable).addRegexMapping(user, domain, regex);
        } catch (InvalidMappingException e) {
            throw new VirtualUserTableManagementException(e);
        }
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagementService#getUserDomainMappings(java.lang.String, java.lang.String, java.lang.String)
     */
    public Collection getUserDomainMappings(String virtualUserTable, String user, String domain) throws VirtualUserTableManagementException {
        try {
            return getTable(virtualUserTable).getUserDomainMappings(user, domain);
        } catch (InvalidMappingException e) {
            throw new VirtualUserTableManagementException(e);
        }
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagementService#removeAddressMapping(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeAddressMapping(String virtualUserTable, String user, String domain, String address) throws VirtualUserTableManagementException {
        try {
            return getTable(virtualUserTable).removeAddressMapping(user, domain, address);
        } catch (InvalidMappingException e) {
            throw new VirtualUserTableManagementException(e);
        }
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagementService#removeErrorMapping(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeErrorMapping(String virtualUserTable, String user, String domain, String error) throws VirtualUserTableManagementException {
        try {
            return getTable(virtualUserTable).removeErrorMapping(user, domain, error);
        } catch (InvalidMappingException e) {
            throw new VirtualUserTableManagementException(e);
        }
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagementService#removeRegexMapping(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeRegexMapping(String virtualUserTable, String user, String domain, String regex) throws VirtualUserTableManagementException {
        try {
            return getTable(virtualUserTable).removeRegexMapping(user, domain, regex);
        } catch (InvalidMappingException e) {
            throw new VirtualUserTableManagementException(e);
        }
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagementService#addMapping(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addMapping(String virtualUserTable, String user, String domain, String mapping) throws VirtualUserTableManagementException {
        try {
            return getTable(virtualUserTable).addMapping(user, domain, mapping);
        } catch (InvalidMappingException e) {
            throw new VirtualUserTableManagementException(e);
        }
    }

    /**
     * @see org.apache.james.services.VirtualUserTableManagementService#removeMapping(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean removeMapping(String virtualUserTable, String user, String domain, String mapping) throws VirtualUserTableManagementException {
        try {
            return getTable(virtualUserTable).removeMapping(user, domain, mapping);
        } catch (InvalidMappingException e) {
            throw new VirtualUserTableManagementException(e);
        }
    }
}
