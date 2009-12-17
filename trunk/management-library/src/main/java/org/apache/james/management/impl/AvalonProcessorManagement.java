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

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.management.ProcessorManagementMBean;
import org.apache.james.management.ProcessorManagementService;
import org.apache.james.services.SpoolManager;
import org.apache.james.bridge.GuiceInjected;

import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonProcessorManagement implements GuiceInjected, Serviceable, Initializable, ProcessorManagementService, ProcessorManagementMBean {

    private ProcessorManagement mgmt;
    private SpoolManager smanager;
    
    public void service(ServiceManager manager) throws ServiceException {
        smanager = (SpoolManager)manager.lookup(SpoolManager.ROLE);

    }

    public void initialize() throws Exception {
        mgmt = Guice.createInjector(new Jsr250Module(), new AbstractModule() {

            @Override
            protected void configure() {
                bind(SpoolManager.class).annotatedWith(Names.named("org.apache.james.services.SpoolManager")).toInstance(smanager);
            }
            
        }).getInstance(ProcessorManagement.class);
    }

    public String[] getMailetNames(String processorName) {
        return mgmt.getMailetNames(processorName);
    }

    public String[] getMailetParameters(String processorName, int mailetIndex) {
        return mgmt.getMailetParameters(processorName, mailetIndex);
    }

    public String[] getMatcherNames(String processorName) {
        return mgmt.getMatcherNames(processorName);
    }

    public String[] getMatcherParameters(String processorName, int matcherIndex) {
        return mgmt.getMatcherParameters(processorName, matcherIndex);
    }

    public String[] getProcessorNames() {
        return mgmt.getProcessorNames();
    }

}
