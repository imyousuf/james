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
package org.apache.james.container.spring;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.protocols.api.ExtensibleHandler;
import org.apache.james.protocols.api.HandlersPackage;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.WiringException;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;


/**
 * ProtocolHandlerchain implementation which register all configured Handlers
 * in the BeanFactory of the Spring context.
 * 
 *
 */
@SuppressWarnings("unchecked")
public class SpringProtocolHandlerChain implements BeanFactoryPostProcessor, ProtocolHandlerChain, ApplicationContextAware, ApplicationListener, BeanNameAware {

    private final List<String> handlers = new LinkedList<String>();
    private Registry<HierarchicalConfiguration> confProvider;
    private Registry<Log> logProvider;
    private Log log;
    private ApplicationContext context;
    private String coreHandlersPackage;
    private String name;

    public void setConfigurationRegistry(Registry<HierarchicalConfiguration> confProvider) {
        this.confProvider = confProvider;
    }

    public void setLogRegistry(Registry<Log> logProvider) {
        this.logProvider = logProvider;
    }

    public void setCoreHandlersPackage(String coreHandlersPackage) {
        this.coreHandlersPackage = coreHandlersPackage;
    }
    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory arg0) throws BeansException {
        HierarchicalConfiguration handlerchainConfig;
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) arg0;
        
        try {
            log = logProvider.getForComponent(name);
            handlerchainConfig = confProvider.getForComponent(name).configurationAt("handler.handlerchain");
            if (handlerchainConfig.getString("[@coreHandlersPackage]") == null)
                handlerchainConfig.addProperty("[@coreHandlersPackage]", coreHandlersPackage);
            
            loadHandlers(registry, handlerchainConfig);                      
        } catch (Exception e) {
            throw new FatalBeanException("Unable to instance ProtocolHandlerChain",e);
        }
     
        
    }

    
    /**
     * ExtensibleHandler wiring
     * 
     * @throws WiringException 
     */
    private void wireExtensibleHandlers() throws WiringException {
        String[] beanNames = context.getBeanNamesForType(ExtensibleHandler.class);

        for (int a = 0; a < beanNames.length; a++) {
            String name = beanNames[a];
            
            // check if the bean is one of our handlers
            if (handlers.contains(name)) {
                final ExtensibleHandler extensibleHandler = (ExtensibleHandler) context.getBean(name);
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
    private void loadClass(BeanDefinitionRegistry registry, String className,
            org.apache.commons.configuration.HierarchicalConfiguration config) throws Exception {
        final Class<?> handlerClass = context.getClassLoader().loadClass(className);
        
        
       

        // if it is a commands handler add it to the map with key as command
        // name
        if (HandlersPackage.class.isAssignableFrom(handlerClass)) {
            List<String> c = ((HandlersPackage) handlerClass.newInstance()).getHandlers();

            for (Iterator<String> i = c.iterator(); i.hasNext(); ) {
                String cName = i.next();

                HierarchicalConfiguration cmdConf = addHandler(cName);

                loadClass(registry, cName, cmdConf);
            }

        } else {
            confProvider.registerForComponent(className, config);
            logProvider.registerForComponent(className, log);
            
            registry.registerBeanDefinition(className, BeanDefinitionBuilder.genericBeanDefinition(className).setLazyInit(false).getBeanDefinition());
            // fill the big handler table
            handlers.add(className);
            
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
        hConf.addProperty("handler/@class", className);
        return hConf;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.james.socket.shared.ProtocolHandlerChain#getHandlers(java.lang.Class)
     */
    public <T> LinkedList<T> getHandlers(Class<T> type) {
        LinkedList<T> result = new LinkedList<T>();
        Map<String,T> beans = context.getBeansOfType(type);
        
        for (Iterator<String> i = handlers.iterator(); i.hasNext(); ) {
            String handler = i.next();
            T bean = beans.get(handler);
            if (bean != null) {
                result.add(bean);
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
    private void loadHandlers(BeanDefinitionRegistry registry, HierarchicalConfiguration commonsConf) throws Exception {

            List<org.apache.commons.configuration.HierarchicalConfiguration> children = ((HierarchicalConfiguration) commonsConf).configurationsAt("handler");

            String coreCmdName = commonsConf.getString("[@coreHandlersPackage]");
            // load the core handlers
            loadClass(registry, coreCmdName,
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
                            loadClass(registry, className, hConf);
                        }
                    }
                }
               
            }
        
    }
    

    /*
     * (non-Javadoc)
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }


    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
     */
    public void setBeanName(String name) {        
        this.name = name;
    }


    /*
     * (non-Javadoc)
     * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            try {
                // wire the extensions after the startup of the application was complete
                // This is needed to be sure that every dependency was injected and every bean was
                // initialized
                wireExtensibleHandlers();
            } catch (WiringException e) {
                throw new RuntimeException("Unable to wire handlers", e);
            }
        }
    }
}