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
package org.apache.james.domainlist.lib;

import javax.annotation.Resource;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListManagementMBean;

public class DomainListManagement extends StandardMBean implements DomainListManagementMBean{

    private DomainList domainList;

    public DomainListManagement() throws NotCompliantMBeanException {
        super(DomainListManagementMBean.class);
    }
    
    @Resource(name="domainlist")
    public void setDomainList(DomainList domainList) {
        this.domainList = domainList;
    }
    
    public boolean addDomain(String domain) {
        return domainList.addDomain(domain);
    }

    public boolean containsDomain(String domain) {
        return domainList.containsDomain(domain);
    }

    public String[] getDomains() {
        return domainList.getDomains();
    }

    public boolean removeDomain(String domain) {
        return domainList.removeDomain(domain);
    }

}
