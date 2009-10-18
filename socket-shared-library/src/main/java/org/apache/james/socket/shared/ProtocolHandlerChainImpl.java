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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.api.protocol.Configurable;
import org.apache.james.api.protocol.ExtensibleHandler;
import org.apache.james.api.protocol.HandlersPackage;
import org.apache.james.api.protocol.LogEnabled;
import org.apache.james.api.protocol.ProtocolHandlerChain;
import org.apache.james.api.protocol.WiringException;

/**
 * Abstract class which HandlerChains should extend
 * 
 *
 */
public class ProtocolHandlerChainImpl implements LogEnabled, Configurable, ProtocolHandlerChain {
    
    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(ProtocolHandlerChainImpl.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log log = FALLBACK_LOG;
   
    /**
     * Sets the service log.
     * Where available, a context sensitive log should be used.
     * @param Log not null
     */
    public void setLog(Log log) {
        this.log = log;
    }

    /**
     * @see org.apache.james.socket.shared.ProtocolHandlerChainImpl#getLog()
     */
    protected Log getLog() {
        return log;
    }

    protected final List<Object> handlers = new LinkedList<Object>();
    
    /** Loads instances */
    private LoaderService loader;

    protected Configuration commonsConf;
    
    
    /**
     * Gets the current instance loader.
     * @return the loader
     */
    public final LoaderService getLoader() {
        return loader;
    }

    /**
     * Sets the loader to be used for instances.
     * @param loader the loader to set, not null
     */
    @Resource(name="org.apache.james.LoaderService")
    public final void setLoader(LoaderService loader) {
        this.loader = loader;
    }
    
    
    /**
     * ExtensibleHandler wiring
     * 
     * @throws WiringException 
     */
    protected void wireExtensibleHandlers() throws WiringException {
        for (Iterator<?> h = handlers.iterator(); h.hasNext(); ) {
            Object handler = h.next();
            if (handler instanceof ExtensibleHandler) {
                final ExtensibleHandler extensibleHandler = (ExtensibleHandler) handler;
                final List<Class<?>> markerInterfaces = extensibleHandler.getMarkerInterfaces();
                for (int i= 0;i < markerInterfaces.size(); i++) {
                    final Class<?> markerInterface = markerInterfaces.get(i);
                    final List<?> extensions = getHandlers(markerInterface);
                    extensibleHandler.wireExtensions(markerInterface,extensions);
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
    protected void loadClass(ClassLoader classLoader, String className,
            org.apache.commons.configuration.Configuration config) throws Exception {
        final Class<?> handlerClass = classLoader.loadClass(className);
        Object handler = loader.load(handlerClass);

        // enable logging
        if (handler instanceof LogEnabled) {
            ((LogEnabled) handler).setLog(getLog());
        }

        // configure the handler
        if (handler instanceof org.apache.james.api.protocol.Configurable) {
            org.apache.james.api.protocol.Configurable configurableHandler = (org.apache.james.api.protocol.Configurable) handler;
            configurableHandler.configure(config);
        }

        // if it is a commands handler add it to the map with key as command
        // name
        if (handler instanceof HandlersPackage) {
            List<String> c = ((HandlersPackage) handler).getHandlers();

            for (Iterator<String> i = c.iterator(); i.hasNext(); ) {
                String cName = i.next();

                Configuration cmdConf = addHandler(cName);

                loadClass(classLoader, cName, cmdConf);
            }

        }

        if (getLog().isInfoEnabled()) {
            getLog().info("Added Handler: " + className);
        }

        // fill the big handler table
        handlers.add(handler);
    }
    
    /**
     * Return a DefaultConfiguration build on the given command name and classname
     * 
     * @param cmdName The command name
     * @param className The class name
     * @return DefaultConfiguration
     * @throws ConfigurationException 
     */
    protected Configuration addHandler(String className) throws ConfigurationException {
        Configuration hConf = new BaseConfiguration();
        hConf.addProperty("handler/@class", className);
        return hConf;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.james.socket.shared.ProtocolHandlerChain#getHandlers(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <T> LinkedList<T> getHandlers(Class<T> type) {
        LinkedList<T> result = new LinkedList<T>();
        for (Iterator<?> i = handlers.iterator(); i.hasNext(); ) {
            Object handler = i.next();
            if (type.isInstance(handler)) {
                result.add((T)handler);
            }
        }
        return result;
    }

    /**
     * loads the various handlers from the configuration
     * 
     * @param configuration
     *            configuration under handlerchain node
     */
    @SuppressWarnings("unchecked")
    protected void loadHandlers() throws Exception {
        if (commonsConf != null && commonsConf instanceof HierarchicalConfiguration) {
            
            List<org.apache.commons.configuration.Configuration> children = ((HierarchicalConfiguration) commonsConf).configurationsAt("handler");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            String coreCmdName = commonsConf.getString("@coreHandlersPackage");
            // load the core handlers
            loadClass(classLoader, coreCmdName,
                    addHandler(coreCmdName));

            // load the configured handlers
            if (children != null && children.isEmpty() == false) {

                for (int i = 0; i < children.size(); i++) {
                    org.apache.commons.configuration.Configuration hConf = children.get(i);
                    String className = hConf.getString("@class");

                    if (className != null) {
                        // ignore base handlers.
                        if (!className.equals(coreCmdName)) {

                            // load the handler
                            loadClass(classLoader, className, hConf);
                        }
                    }
                }
               
            }
        }
    }
    
    /**
     * Configure the chain
     * 
     * @param commonsConf
     */
    public void configure(Configuration commonsConf) throws ConfigurationException {
        this.commonsConf =  commonsConf;
        try {
            loadHandlers();
            wireExtensibleHandlers();
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage(), e);
        }    
    }
    
}
