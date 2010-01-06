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
package org.apache.james.socket.shared;

import java.net.UnknownHostException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.socket.api.ProtocolHandlerFactory;

/**
 * Abstract base class which ProtocolHandlerFactory implementation should extend
 *
 */
public abstract class AbstractProtocolHandlerFactory implements ProtocolHandlerFactory, LogEnabled, Configurable{
    
    
    private DNSService dnsService;
    private Log log;
    

    /**
     * The name of the parameter defining the service hello name.
     */
    private static final String HELLO_NAME = "helloName";
    
    private String helloName;

    /**
     * Sets the DNS service.
     * @param dnsServer the dnsServer to set
     */
    @Resource(name="dnsserver")
    public final void setDNSService(DNSService dnsServer) {
        this.dnsService = dnsServer;
    }

    protected final DNSService getDNSService() {
        return dnsService;
    }
    
    public void configure(HierarchicalConfiguration configuration) throws ConfigurationException{

        configureHelloName(configuration.configurationAt("handler"));
        onConfigure(configuration);
    }
    
    
    public void setLog(Log logger) {
        this.log = logger;
    }
    
    @PostConstruct
    public void init() throws Exception {
        onInit();
    }

    
    private void configureHelloName(HierarchicalConfiguration handlerConfiguration) {
        StringBuilder infoBuffer;
        String hostName = null;
        try {
            hostName = dnsService.getHostName(dnsService.getLocalHost());
        } catch (UnknownHostException ue) {
            hostName = "localhost";
        }

        infoBuffer =
            new StringBuilder(64)
                    .append(getServiceType())
                    .append(" is running on: ")
                    .append(hostName);
        log.info(infoBuffer.toString());

        
 
        if (handlerConfiguration.getKeys(HELLO_NAME).hasNext()) {
            boolean autodetect = handlerConfiguration.getBoolean(HELLO_NAME +".[@autodetect]", true);
            if (autodetect) {
                helloName = hostName;
            } else {
                // Should we use the defaultdomain here ?
                helloName = handlerConfiguration.getString(HELLO_NAME, "localhost");
            }
        } else {
            helloName = null;
        }
        infoBuffer =
            new StringBuilder(64)
                    .append(getServiceType())
                    .append(" handler hello name is: ")
                    .append(helloName);
        log.info(infoBuffer.toString());
    }
    
    protected Log getLogger() {
        return log;
    }
    
    protected String getHelloName() {
        return helloName;
    }
    

    /**
     * Override this method for init
     * 
     * @throws Exception
     */
    protected void onInit() throws Exception {
        
    }
    
    /**
     * Override this method for handle extra configuration
     * 
     * @param config
     * @throws ConfigurationException
     */
    protected void onConfigure(HierarchicalConfiguration config) throws ConfigurationException {
        
    }
}
