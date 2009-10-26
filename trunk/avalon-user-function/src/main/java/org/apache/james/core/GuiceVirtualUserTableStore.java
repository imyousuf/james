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



package org.apache.james.core;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.james.services.FileSystem;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;

/**
 * Provides a registry of VirtualUserTables
 *
 */
public class GuiceVirtualUserTableStore
    extends AbstractGuiceStore<VirtualUserTable>
    implements VirtualUserTableStore {

    private FileSystem fs;
    private DNSService dns;
    private DataSourceSelector selector;


    // TODO: REMOVE ME!!
    @PostConstruct
    @Override
    public void init() throws Exception {
        super.init();
    }

    @Resource(name="org.apache.avalon.cornerstone.services.datasources.DataSourceSelector")
    public void setDataSourceSelector(DataSourceSelector selector) {
        this.selector = selector;
    }
    
    
    @Resource(name="org.apache.james.services.FileSystem")
    public void setFileSystem(FileSystem fs) {
        this.fs = fs;
    }
    
    @Resource(name="org.apache.james.api.dnsservice.DNSService")
    public void setDNSService(DNSService dns) {
        this.dns = dns;
    }
    
    /** 
     * Get the repository, if any, whose name corresponds to
     * the argument parameter
     *
     * @param name the name of the desired repository
     *
     * @return the VirtualUserTable corresponding to the name parameter
     */
    public VirtualUserTable getTable(String name) {
        VirtualUserTable response = getObject(name);
        if ((response == null) && (getLogger().isWarnEnabled())) {
            getLogger().warn("No virtualUserTable called: " + name);
        }
        return response;
    }

    /**
     * @see org.apache.james.core.AbstractAvalonStore#getStoreName()
     */
    public String getStoreName() {
        return "AvalonVirtualUserTableStore";
    }


    /**
     * @see org.apache.james.core.AbstractGuiceStore#getConfigurations(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    @SuppressWarnings("unchecked")
    public List<HierarchicalConfiguration> getConfigurations(
            HierarchicalConfiguration config) {
        return config.configurationsAt("table");
    }


    @Override
    protected Module getModule() {
        return new AbstractModule() {
            
            @Override
            protected void configure() {
                bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
                bind(DataSourceSelector.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.datasources.DataSourceSelector")).toInstance(selector);
                bind(FileSystem.class).annotatedWith(Names.named("org.apache.james.services.FileSystem")).toInstance(fs);
                bind(DNSService.class).annotatedWith(Names.named("org.apache.james.api.dnsservice.DNSService")).toInstance(dns);
            }
        };
    }
}
