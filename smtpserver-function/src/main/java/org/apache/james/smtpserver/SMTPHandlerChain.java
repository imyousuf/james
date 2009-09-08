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



package org.apache.james.smtpserver;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.smtpserver.core.CoreCmdHandlerLoader;
import org.apache.james.smtpserver.core.CoreMessageHookLoader;

/**
  * The SMTPHandlerChain is per service object providing access
  * ConnectHandlers, Commandhandlers and message handlers
  */
public class SMTPHandlerChain extends AbstractLogEnabled implements Configurable, Serviceable {

    /** Configuration for this chain */
    private Configuration configuration;
    private org.apache.commons.configuration.Configuration commonsConf;
    
    private List<Object> handlers = new LinkedList<Object>();

    private ServiceManager serviceManager;
    
    /** Loads instances */
    private LoaderService loader;
    
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
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager arg0) throws ServiceException {
        serviceManager = arg0;
    }

    /**
     * ExtensibleHandler wiring
     * 
     * @throws WiringException 
     */
    private void wireExtensibleHandlers() throws WiringException {
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
     * loads the various handlers from the configuration
     * 
     * @param configuration
     *            configuration under handlerchain node
     */
    public void configure(Configuration configuration)
            throws ConfigurationException {
        if (configuration == null
                || configuration.getChildren("handler") == null
                || configuration.getChildren("handler").length == 0) {
            configuration = new DefaultConfiguration("handlerchain");
            Properties cmds = new Properties();
            cmds.setProperty("Default CoreCmdHandlerLoader", CoreCmdHandlerLoader.class
                    .getName());
            Enumeration<Object> e = cmds.keys();
            while (e.hasMoreElements()) {
                String cmdName = (String) e.nextElement();
                String className = cmds.getProperty(cmdName);
                ((DefaultConfiguration) configuration).addChild(addHandler(
                        cmdName, className));
            }
        }
        try {
			commonsConf = new JamesConfiguration(configuration);
		} catch (org.apache.commons.configuration.ConfigurationException e) {
			throw new ConfigurationException("Unable to wrap configuration",e);
		}
        this.configuration = configuration;
        
    }

    private void loadHandlers() throws Exception {
        if (configuration != null) {
            Configuration[] children = configuration.getChildren("handler");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // load the configured handlers
            if (children != null) {

                // load the core handlers
                loadClass(classLoader, CoreCmdHandlerLoader.class.getName(),
                        addHandler(null, CoreCmdHandlerLoader.class.getName()));
                
                for (int i = 0; i < children.length; i++) {
                    String className = children[i].getAttribute("class");
                    if (className != null) {

                        // ignore base handlers.
                        if (!className.equals(CoreCmdHandlerLoader.class
                                        .getName())) {

                            // load the handler
                            loadClass(classLoader, className, children[i]);
                        }
                    }
                }
                // load core messageHandlers
                loadClass(classLoader, CoreMessageHookLoader.class.getName(),
                        addHandler(null, CoreMessageHookLoader.class.getName()));

            }
        }
    }
    
    /**
     * Initializes chain.
     */
    public void initialize() throws Exception {
//        SMTPCommandDispatcherLineHandler commandDispatcherLineHandler = new SMTPCommandDispatcherLineHandler();
//        commandDispatcherLineHandler.enableLogging(getLogger());
//        handlers.add(commandDispatcherLineHandler);
        
        loadHandlers();
        
        Iterator<Object> h = handlers.iterator();
    
        while(h.hasNext()) {
            Object next = h.next();
            ContainerUtil.initialize(next);
        }
        wireExtensibleHandlers();

    }

    /**
     * Load and add the classes to the handler map
     * 
     * @param classLoader The classLoader to use
     * @param className The class name 
     * @param config The configuration 
     * @throws ConfigurationException Get thrown on error
     */
    private void loadClass(ClassLoader classLoader, String className,
            Configuration config) throws Exception {
        final Class<?> handlerClass = classLoader.loadClass(className);
        Object handler = loader.load(handlerClass);

        // enable logging
        ContainerUtil.enableLogging(handler, getLogger());

        // servicing the handler
        ContainerUtil.service(handler, serviceManager);

        // configure the handler
        
        // will get removed after removing of avalon for config is complete
        ContainerUtil.configure(handler, config);
        
        if (handler instanceof org.apache.james.smtpserver.Configurable) {
        	((org.apache.james.smtpserver.Configurable) handler).configure(commonsConf);
        }

        // if it is a commands handler add it to the map with key as command
        // name
        if (handler instanceof HandlersPackage) {
            List<String> c = ((HandlersPackage) handler).getHandlers();

            for (Iterator<String> i = c.iterator(); i.hasNext(); ) {
                String cName = i.next();

                DefaultConfiguration cmdConf = new DefaultConfiguration(
                        "handler");
                cmdConf.setAttribute("class", cName);

                loadClass(classLoader, cName, cmdConf);
            }

        }

        if (getLogger().isInfoEnabled()) {
            getLogger().info("Added Handler: " + className);
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
     */
    private DefaultConfiguration addHandler(String cmdName, String className) {
        DefaultConfiguration cmdConf = new DefaultConfiguration("handler");
        cmdConf.setAttribute("command",cmdName);
        cmdConf.setAttribute("class",className);
        return cmdConf;
    }
    
    /**
     * Returns a list of handler of the requested type.
     * @param <T>
     * 
     * @param type the type of handler we're interested in
     * @return a List of handlers
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

}
