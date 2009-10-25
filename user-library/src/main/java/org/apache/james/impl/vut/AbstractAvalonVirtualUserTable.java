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

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.management.InvalidMappingException;
import org.apache.james.api.vut.management.VirtualUserTableManagement;
import org.apache.james.smtpserver.mina.GuiceInjected;
import org.apache.james.util.ConfigurationAdapter;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public abstract class AbstractAvalonVirtualUserTable implements Serviceable, Initializable, Configurable, GuiceInjected, LogEnabled, VirtualUserTable, VirtualUserTableManagement, DomainList{

    private DNSService dns;
    private HierarchicalConfiguration config;
    private Log log;
    
    protected abstract AbstractVirtualUserTable getVirtualUserTable();
    
    
    public void service(ServiceManager manager) throws ServiceException {
        dns = (DNSService) manager.lookup(DNSService.ROLE);
    }


    public void enableLogging(Logger arg0) {
        log = new AvalonLogger(arg0);
    }


    public void configure(Configuration arg0) throws ConfigurationException {
        try {
            config = new ConfigurationAdapter(arg0);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Unable to convert config",e);
        }
    }


    public Collection<String> getMappings(String user, String domain) throws ErrorMappingException {
        return getVirtualUserTable().getMappings(user, domain);
    }

    public boolean addAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        return getVirtualUserTable().addAddressMapping(user, domain, address);
    }

    public boolean addAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException {
        return getVirtualUserTable().addAliasDomainMapping(aliasDomain, realDomain);
    }

    public boolean addErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        return getVirtualUserTable().addErrorMapping(user, domain, error);
    }

    public boolean addMapping(String user, String domain, String mapping) throws InvalidMappingException {
        return getVirtualUserTable().addMapping(user, domain, mapping);
    }

    public boolean addRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        return getVirtualUserTable().addRegexMapping(user, domain, regex);
    }

    public Map<String, Collection<String>> getAllMappings() {
        return getVirtualUserTable().getAllMappings();
    }

    public Collection<String> getUserDomainMappings(String user, String domain) throws InvalidMappingException {
        return getVirtualUserTable().getUserDomainMappings(user, domain);
    }

    public boolean removeAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        return getVirtualUserTable().removeAddressMapping(user, domain, address);
    }

    public boolean removeAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException {
        return getVirtualUserTable().removeAliasDomainMapping(aliasDomain, realDomain);
    }

    public boolean removeErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        return getVirtualUserTable().removeErrorMapping(user, domain, error);
    }

    public boolean removeMapping(String user, String domain, String mapping) throws InvalidMappingException {
        return getVirtualUserTable().removeMapping(user, domain, mapping);
    }

    public boolean removeRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        return getVirtualUserTable().removeRegexMapping(user, domain, regex);
    }

    public boolean containsDomain(String domain) {
        return getVirtualUserTable().containsDomain(domain);
    }

    public List<String> getDomains() {
        return getVirtualUserTable().getDomains();
    }

    public void setAutoDetect(boolean autodetect) {
        getVirtualUserTable().setAutoDetect(autodetect);
    }

    public void setAutoDetectIP(boolean autodetectIP) {
        getVirtualUserTable().setAutoDetectIP(autodetectIP);
    }
    
    public class BaseAvalonVirtualUserTableModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(DNSService.class).annotatedWith(Names.named("org.apache.james.api.dnsservice.DNSService")).toInstance(dns);
            bind(HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
            bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(log);
        }
        
    }
}
