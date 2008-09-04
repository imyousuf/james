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



package org.apache.james.management.impl;

import java.util.List;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.api.domainlist.ManageableDomainList;
import org.apache.james.management.DomainListManagementException;
import org.apache.james.management.DomainListManagementMBean;
import org.apache.james.management.DomainListManagementService;

/**
 * Provide management class for DomainLists
 */
public class DomainListManagement implements DomainListManagementService,DomainListManagementMBean,Serviceable {
    private DomainList domList;
    
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        setDomainList((DomainList) arg0.lookup(DomainList.ROLE));
    }
    
    public void setDomainList(DomainList domList) {
        this.domList = domList;
    }
    
    /**
     * @see org.apache.james.management.DomainListManagementService#addDomain(java.lang.String)
     */
    public boolean addDomain(String domain) throws DomainListManagementException {
        if (domList instanceof ManageableDomainList) {
            try {
                return ((ManageableDomainList)domList).addDomain(domain);
            } catch (UnsupportedOperationException e) {
                //TODO: Remove later. Temporary fix
                throw new DomainListManagementException(e);
            } 
        } else {
            throw new DomainListManagementException("Used DomainList implementation not support management");
        }
    }

    /**
     * @see org.apache.james.management.DomainListManagementService#removeDomain(java.lang.String)
     */
    public boolean removeDomain(String domain) throws DomainListManagementException {
        if (domList instanceof ManageableDomainList) {
            try {
                return ((ManageableDomainList)domList).removeDomain(domain);
            } catch (UnsupportedOperationException e) {
                //TODO: Remove later. Temporary fix
                throw new DomainListManagementException(e);
            }
        } else {
           throw new DomainListManagementException("Used DomainList implementation not support management");
        }
    }

    /**
     * @see org.apache.james.management.DomainListManagementService#containsDomain(java.lang.String)
     */
    public boolean containsDomain(String domain) {
        return domList.containsDomain(domain);
    }

    /**
     * @see org.apache.james.management.DomainListManagementService#getDomains()
     */
    public List getDomains() {
        return domList.getDomains();
    }
}
