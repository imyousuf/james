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

import javax.mail.MessagingException;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.management.SpoolFilter;
import org.apache.james.management.SpoolManagementException;
import org.apache.james.management.SpoolManagementMBean;
import org.apache.james.management.SpoolManagementService;
import org.apache.james.bridge.GuiceInjected;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonSpoolManagement  implements GuiceInjected, Serviceable, Initializable, SpoolManagementService, SpoolManagementMBean{

    private SpoolManagement mgmt;
    private Store mailStore;
    public void service(ServiceManager manager) throws ServiceException {
        mailStore = (Store)manager.lookup(Store.ROLE);
        
    }

    public List<String> getSpoolItems(String spoolRepositoryURL, SpoolFilter filter) throws MessagingException, SpoolManagementException {
        return mgmt.getSpoolItems(spoolRepositoryURL, filter);
    }

    public int moveSpoolItems(String srcSpoolRepositoryURL, String dstSpoolRepositoryURL, String dstState, SpoolFilter filter) throws MessagingException, SpoolManagementException {
        return mgmt.moveSpoolItems(srcSpoolRepositoryURL, dstSpoolRepositoryURL, dstState, filter);
    }

    public int removeSpoolItems(String spoolRepositoryURL, String key, List<String> lockingFailures, SpoolFilter filter) throws MessagingException, SpoolManagementException {
        return mgmt.removeSpoolItems(spoolRepositoryURL, key, filter);
    }

    public int resendSpoolItems(String spoolRepositoryURL, String key, List<String> lockingFailures, SpoolFilter filter) throws MessagingException, SpoolManagementException {
        return mgmt.resendSpoolItems(spoolRepositoryURL, key, filter);
    }

    public String[] listSpoolItems(String spoolRepositoryURL, String state, String header, String headerValueRegex) throws SpoolManagementException {
        return mgmt.listSpoolItems(spoolRepositoryURL, state, header, headerValueRegex);
    }

    public int moveSpoolItems(String srcSpoolRepositoryURL, String srcState, String dstSpoolRepositoryURL, String dstState, String header, String headerValueRegex) throws SpoolManagementException {
        return mgmt.moveSpoolItems(srcSpoolRepositoryURL, srcState, dstSpoolRepositoryURL, dstState, header, headerValueRegex);
    }

    public int removeSpoolItems(String spoolRepositoryURL, String key, String state, String header, String headerValueRegex) throws SpoolManagementException {
        return mgmt.removeSpoolItems(spoolRepositoryURL, key, state, header, headerValueRegex);
    }

    public int resendSpoolItems(String spoolRepositoryURL, String key, String state, String header, String headerValueRegex) throws SpoolManagementException {
        return mgmt.resendSpoolItems(spoolRepositoryURL, key, state, header, headerValueRegex);
    }

    public void initialize() throws Exception {
        mgmt = Guice.createInjector(new Jsr250Module(), new AbstractModule() {

            @Override
            protected void configure() {
                bind(Store.class).annotatedWith(Names.named("mailstore")).toInstance(mailStore);
            }
            
        }).getInstance(SpoolManagement.class);
    }

}
