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
package org.apache.james.dnsserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.dnsservice.TemporaryResolutionException;
import org.apache.james.bridge.GuiceInjected;
import org.apache.james.util.ConfigurationAdapter;
import org.apache.mailet.HostAddress;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonDNSServer implements GuiceInjected, DNSService, Configurable, Initializable, LogEnabled, DNSServerMBean{
    private DNSServer dns;
    private HierarchicalConfiguration config;
    private Log logger;
    
    public Collection<String> findMXRecords(String hostname)
            throws TemporaryResolutionException {
        return dns.findMXRecords(hostname);
    }

    public Collection<String> findTXTRecords(String hostname) {
        return dns.findTXTRecords(hostname);
    }

    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        return dns.getAllByName(host);
    }

    public InetAddress getByName(String host) throws UnknownHostException {
        return dns.getByName(host);
    }

    public String getHostName(InetAddress addr) {
        return dns.getHostName(addr);
    }

    public InetAddress getLocalHost() throws UnknownHostException {
        return dns.getLocalHost();
    }

    public Iterator<HostAddress> getSMTPHostAddresses(String domainName)
            throws TemporaryResolutionException {
        return dns.getSMTPHostAddresses(domainName);
    }

    public void configure(Configuration arg0) throws ConfigurationException {
        try {
            config = new ConfigurationAdapter(arg0);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Unable to convert config");
        }
    }

    public void initialize() throws Exception {
        dns = Guice.createInjector(new Jsr250Module(), new AbstractModule() {
            
            @Override
            protected void configure() {
                bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
                bind(HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);

            }
        }).getInstance(DNSServer.class);
    }

    public String[] getDNSServers() {
        return dns.getDNSServers();
    }

    public void enableLogging(Logger arg0) {
        logger = new AvalonLogger(arg0);
    }

}
