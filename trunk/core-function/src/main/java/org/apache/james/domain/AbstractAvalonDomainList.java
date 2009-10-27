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
package org.apache.james.domain;

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
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.domainlist.ManageableDomainList;
import org.apache.james.bridge.GuiceInjected;
import org.apache.james.util.ConfigurationAdapter;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public abstract class AbstractAvalonDomainList implements Initializable, LogEnabled, Configurable, GuiceInjected, Serviceable, ManageableDomainList {

    protected ManageableDomainList domList;
    private HierarchicalConfiguration config;
    private Log logger;
    private DNSService dns;

    public void enableLogging(Logger arg0) {
        logger = new AvalonLogger(arg0);
    }


    public void configure(Configuration arg0) throws ConfigurationException {
        try {
            config = new ConfigurationAdapter(arg0);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Unable to convert config");
        }
    }

    public void service(ServiceManager arg0) throws ServiceException {
        dns = (DNSService) arg0.lookup(DNSService.ROLE);
    }

    public boolean addDomain(String domain) {
        return domList.addDomain(domain);
    }

    public boolean removeDomain(String domain) {
        return domList.removeDomain(domain);
    }

    public boolean containsDomain(String domain) {
        return domList.containsDomain(domain);
    }

    public List<String> getDomains() {
        return domList.getDomains();
    }

    public void setAutoDetect(boolean autodetect) {
        domList.setAutoDetect(autodetect);
    }

    public void setAutoDetectIP(boolean autodetectIP) {
        domList.setAutoDetectIP(autodetectIP);
    }

    public class AbstractAvalonDomainModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
            bind(HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
            bind(DNSService.class).annotatedWith(Names.named("org.apache.james.api.dnsservice.DNSService")).toInstance(dns);
        }
        
    }
}
