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
package org.apache.james.server.mock;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.protocols.api.ExtensibleHandler;
import org.apache.james.protocols.api.HandlersPackage;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.WiringException;


/**
 * ProtocolHandlerchain implementation which instance all the configured handlers
 * 
 *
 * TODO: Move this to test package as it is the only place where it get used
 */
@SuppressWarnings("unchecked")
public class MockProtocolHandlerChain implements ProtocolHandlerChain, Configurable, LogEnabled {
  
    private Log log;
    private String coreHandlersPackage;
    private LinkedList handlers = new LinkedList();
    private HierarchicalConfiguration config;
    private MockJSR250Loader factory;


    public void setCoreHandlersPackage(String coreHandlersPackage) {
        this.coreHandlersPackage = coreHandlersPackage;
    }
   
    public void setLoader(MockJSR250Loader factory) {
        this.factory = factory;
    }
    
    
    @PostConstruct
    public void init() throws Exception {
        HierarchicalConfiguration handlerchainConfig = config.configurationAt("handlerchain");
        if (handlerchainConfig.getString("[@coreHandlersPackage]") == null)
            handlerchainConfig.addProperty("[@coreHandlersPackage]", coreHandlersPackage);
        
        loadHandlers(handlerchainConfig);     
        
        wireExtensibleHandlers();
    }
    
    /**
     * ExtensibleHandler wiring
     * 
     * @throws WiringException 
     */
    private void wireExtensibleHandlers() throws WiringException {
        for (int a = 0; a < handlers.size(); a++) {
            final Object obj = handlers.get(a);
            if (obj instanceof ExtensibleHandler) {
                final ExtensibleHandler extensibleHandler = (ExtensibleHandler) obj;
                final List<Class<?>> markerInterfaces = extensibleHandler.getMarkerInterfaces();
                for (int i = 0; i < markerInterfaces.size(); i++) {
                    final Class<?> markerInterface = markerInterfaces.get(i);
                    final List<?> extensions = getHandlers(markerInterface);
                    extensibleHandler.wireExtensions(markerInterface, extensions);
                }
            }

        }

    }
    
    /**
     * Load and add the classes to the handler map
     * 
     * @param classLoader The classLoader to use
     * @param className The class name 
     * @param config The configuration 
     * @throws ConfigurationException Get thrown on error
     */
    private void loadClass(String className, org.apache.commons.configuration.HierarchicalConfiguration config) throws Exception {
        Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        Object obj = factory.newInstance(clazz, log, config);


        // if it is a commands handler add it to the map with key as command
        // name
        if (obj instanceof HandlersPackage) {

            List<String> c = ((HandlersPackage) obj).getHandlers();

            for (Iterator<String> i = c.iterator(); i.hasNext(); ) {
                String cName = i.next();

                HierarchicalConfiguration cmdConf = addHandler(cName);

                loadClass(cName, cmdConf);
            }

        } else {
            handlers.add(obj);
            
            if (log.isInfoEnabled()) {
                log.info("Added Handler: " + className);
            }

        }

    }
    
    /**
     * Return a DefaultConfiguration build on the given command name and classname
     * 
     * @param cmdName The command name
     * @param className The class name
     * @return DefaultConfiguration
     * @throws ConfigurationException 
     */
    private HierarchicalConfiguration addHandler(String className) throws ConfigurationException {
        HierarchicalConfiguration hConf = new DefaultConfigurationBuilder();
        hConf.addProperty("handler.[@class]", className);
        return hConf;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.james.socket.shared.ProtocolHandlerChain#getHandlers(java.lang.Class)
     */
    public <T> LinkedList<T> getHandlers(Class<T> type) {
        LinkedList<T> classHandlers = new LinkedList<T>();
        Iterator hList = handlers.iterator();
        while (hList.hasNext()) {
            Object obj = hList.next();
            if (type.isInstance(obj)) {
                classHandlers.add((T)obj);
            }
        }
        return classHandlers;
    }

    /**
     * loads the various handlers from the configuration
     * 
     * @param configuration
     *            configuration under handlerchain node
     */
    private void loadHandlers(HierarchicalConfiguration commonsConf) throws Exception {

            List<org.apache.commons.configuration.HierarchicalConfiguration> children = ((HierarchicalConfiguration) commonsConf).configurationsAt("handler");

            String coreCmdName = commonsConf.getString("[@coreHandlersPackage]");
            // load the core handlers
            loadClass(coreCmdName,
                    addHandler(coreCmdName));

            // load the configured handlers
            if (children != null && children.isEmpty() == false) {

                for (int i = 0; i < children.size(); i++) {
                    org.apache.commons.configuration.HierarchicalConfiguration hConf = children.get(i);
                    String className = hConf.getString("[@class]");

                    if (className != null) {
                        // ignore base handlers.
                        if (!className.equals(coreCmdName)) {

                            // load the handler
                            loadClass(className, hConf);
                        }
                    }
                }
               
            }
        
    }
    

  
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        this.config = config;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.log = log;
    }
}