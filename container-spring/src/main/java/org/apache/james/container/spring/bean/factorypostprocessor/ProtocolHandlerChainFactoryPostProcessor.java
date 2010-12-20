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
package org.apache.james.container.spring.bean.factorypostprocessor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.container.spring.provider.configuration.ConfigurationProvider;
import org.apache.james.container.spring.provider.log.LogProvider;
import org.apache.james.protocols.api.ExtensibleHandler;
import org.apache.james.protocols.api.HandlersPackage;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 *
 * {@link ProtocolHandlerChain} implementation which will parse a configuration file and register all configured handlers in the Spring {@link ConfigurableListableBeanFactory} instance
 * Here the @class attribute of the handler configuration will be used as bean name prefixed with the value of {@link #setBeanName(String)} + :
 * 
 * 
 * This implementation take also care of wire the {@link ExtensibleHandler} for which it is responsible
 * 
 * 
 */
@SuppressWarnings("unchecked")
public abstract class ProtocolHandlerChainFactoryPostProcessor implements ProtocolHandlerChain, BeanFactoryPostProcessor {

    private String coreHandlersPackage;
    
    private List<String> handlers = new LinkedList<String>();
    
    private String beanname;
    
    private ConfigurableListableBeanFactory beanFactory;
    
    public void setBeanName(String beanname) {
        this.beanname = beanname;
    }

    public void setCoreHandlersPackage(String coreHandlersPackage) {
        this.coreHandlersPackage = coreHandlersPackage;
    }

    /**
     * Return a DefaultConfiguration build on the given command name and
     * classname
     * 
     * @param cmdName
     *            The command name
     * @param className
     *            The class name
     * @return DefaultConfiguration
     * @throws ConfigurationException
     */
    private HierarchicalConfiguration addHandler(String className) throws ConfigurationException {
        HierarchicalConfiguration hConf = new DefaultConfigurationBuilder();
        hConf.addProperty("[@class]", className);
        return hConf;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolHandlerChain#getHandlers(java.lang.Class)
     */
    public <T> LinkedList<T> getHandlers(Class<T> type) {
        LinkedList<T> classHandlers = new LinkedList<T>();
        String[] names = beanFactory.getBeanNamesForType(type);

        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            // check if the handler is registered in the handler chain
            if (handlers.contains(name)) {
                classHandlers.add(beanFactory.getBean(name, type));
            }
        }
        
        return classHandlers;
    }
    
    /**
     * Returns the Handlers List.
     * 
     * @return
     */
    public List<String> getHandlers() {
        return handlers;
    }

    /**
     * Lookup the {@link HierarchicalConfiguration} for the beanname which was configured via {@link #setBeanName(String)} and parse it for handlers which should be 
     * registered in the {@link ConfigurableListableBeanFactory}. 
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        this.beanFactory = beanFactory;
        
        ConfigurationProvider confProvider = beanFactory.getBean(ConfigurationProvider.class);
        
        LogProvider logProvider = beanFactory.getBean(LogProvider.class);
        
        try {

            Log log = logProvider.getLog(beanname);
            
            HierarchicalConfiguration config = confProvider.getConfiguration(beanname);
            HierarchicalConfiguration handlerchainConfig = config.configurationAt("handler.handlerchain");
            List<org.apache.commons.configuration.HierarchicalConfiguration> children = handlerchainConfig.configurationsAt("handler");

            // check if the coreHandlersPackage was specified inte hconfig if not add the default 
            if (handlerchainConfig.getString("[@coreHandlersPackage]") == null)
                handlerchainConfig.addProperty("[@coreHandlersPackage]", coreHandlersPackage);

            String coreCmdName = handlerchainConfig.getString("[@coreHandlersPackage]");
            
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

            String coreCmdBeanName = getBeanName(coreCmdName);

            // now register the HandlerPackage 
            BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(coreCmdName).setLazyInit(false).getBeanDefinition();
            registry.registerBeanDefinition(coreCmdBeanName, def);
            HandlersPackage handlersPackage = beanFactory.getBean(coreCmdBeanName, HandlersPackage.class);

            List<String> c = handlersPackage.getHandlers();

            for (Iterator<String> i = c.iterator(); i.hasNext();) {
                String cName = i.next();

                try {
                    HierarchicalConfiguration cmdConf = addHandler(cName);
                    children.add(cmdConf);
                } catch (ConfigurationException e) {
                    throw new FatalBeanException("Unable to create configuration for handler " + cName, e);
                }
            }

            for (int i = 0; i < children.size(); i++) {
                HierarchicalConfiguration hConf = children.get(i);
                String className = hConf.getString("[@class]", null);

                if (className != null) {
                    // ignore base handlers.
                    if (!className.equals(coreCmdName)) {

                        String handlerBeanName = getBeanName(className);

                        // register the log and configuration for it
                        logProvider.registerLog(handlerBeanName, log);
                        confProvider.registerConfiguration(handlerBeanName, hConf);

                        // now register the BeanDefinition on the context and store the beanname for later usage
                        BeanDefinition handlerDef = BeanDefinitionBuilder.genericBeanDefinition(className).getBeanDefinition();
                        registry.registerBeanDefinition(handlerBeanName, handlerDef);
                        
                        handlers.add(handlerBeanName);
                    }
                } else {
                    throw new FatalBeanException("Missing @class attribute in configuration: " + ConfigurationUtils.toString(hConf));
                }
            }
            
            
        } catch (ConfigurationException e) {
            throw new FatalBeanException("Unable to load configuration for bean " + beanname, e);
        }

    }

    private String getBeanName(String name) {
        return beanname + ":" + name;
    }

}
