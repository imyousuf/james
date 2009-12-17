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

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.management.DomainListManagementException;
import org.apache.james.management.DomainListManagementMBean;
import org.apache.james.management.DomainListManagementService;
import org.apache.james.bridge.GuiceInjected;

import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class AvalonDomainListManagement implements GuiceInjected, DomainListManagementService,DomainListManagementMBean, Serviceable, Initializable {
    private DomainList dList;
    private DomainListManagement mgmt;
    public void service(ServiceManager manager) throws ServiceException {
        dList = (DomainList) manager.lookup(DomainList.ROLE);
    }

    public void initialize() throws Exception {
        mgmt = Guice.createInjector(new Jsr250Module(), new AbstractModule() {

            @Override
            protected void configure() {
                bind(DomainList.class).annotatedWith(Names.named("org.apache.james.api.domainlist.DomainList")).toInstance(dList);
            }
            
        }).getInstance(DomainListManagement.class);
    }

    public boolean addDomain(String domain) throws DomainListManagementException {
        return mgmt.addDomain(domain);
    }

    public boolean containsDomain(String domain) {
        return mgmt.containsDomain(domain);
    }

    public List<String> getDomains() {
        return mgmt.getDomains();
    }

    public boolean removeDomain(String domain) throws DomainListManagementException {
        return mgmt.removeDomain(domain);
    }

}
