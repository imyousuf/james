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
package org.apache.james.container.spring.bean;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.protocols.api.ExtensibleHandler;
import org.apache.james.protocols.api.HandlersPackage;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.WiringException;
import org.apache.james.protocols.lib.ConfigurableProtocolHandlerchain;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * 
 * {@link ProtocolHandlerChain} implementation which will parse a configuration
 * file and register all configured handlers in the Spring
 * {@link ConfigurableListableBeanFactory} instance.
 * 
 * Here the @class attribute of the handler configuration will be used as bean
 * name prefixed with the value of {@link #setBeanName(String)} + :
 * 
 * This implementation take also care of wire the {@link ExtensibleHandler} for
 * which it is responsible.
 */
@SuppressWarnings("unchecked")
public class ProtocolHandlerChainImpl implements ConfigurableProtocolHandlerchain, BeanFactoryAware {

    private ConfigurableListableBeanFactory beanFactory;

    private List<Object> handlers = new LinkedList<Object>();

    @Override
    public void init(HierarchicalConfiguration handlerchainConfig) throws ConfigurationException {
        try {
            String jmxName = handlerchainConfig.getString("[@jmxName]");
            List<org.apache.commons.configuration.HierarchicalConfiguration> children = handlerchainConfig.configurationsAt("handler");
            ClassLoader loader = beanFactory.getBeanClassLoader();

   
            String coreCmdName = handlerchainConfig.getString("[@coreHandlersPackage]");

            // now register the HandlerPackage
            // Use the classloader which is used for bean instance stuff
            Class<HandlersPackage> c = (Class<HandlersPackage>) loader.loadClass(coreCmdName);
            HandlersPackage handlersPackage = beanFactory.createBean(c);


            registerHandlersPackage(handlersPackage, null, children);

            if (handlerchainConfig.getBoolean("[@enableJmx]", true)) {
                String jmxHandlerPackage = handlerchainConfig.getString("[@jmxHandlersPackage]");

                // now register the HandlerPackage for jmx
                Class<HandlersPackage> jC = (Class<HandlersPackage>) loader.loadClass(jmxHandlerPackage);
                HandlersPackage jmxPackage = beanFactory.createBean(jC);

                DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
                builder.addProperty("jmxName", jmxName);
                registerHandlersPackage(jmxPackage, builder, children);
            }

            for (int i = 0; i < children.size(); i++) {
                HierarchicalConfiguration hConf = children.get(i);
                String className = hConf.getString("[@class]", null);

                if (className != null) {
                    // ignore base handlers.
                    if (!className.equals(coreCmdName)) {

                        // Use the classloader which is used for bean instance stuff
                        Class<?> clazz = (Class<?>) loader.loadClass(className);
                        Object handler = beanFactory.createBean(clazz);
                        if (handler instanceof Configurable) {
                            ((Configurable) handler).configure(hConf);
                        }
                        handlers.add(handler);
                    }
                } else {
                    throw new FatalBeanException("Missing @class attribute in configuration: " + ConfigurationUtils.toString(hConf));
                }
            }
            
            wireHandlers();
        } catch (WiringException e) {
            throw new ConfigurationException(e);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Unable to load handlers", e);
        }
    }

    private void wireHandlers() throws WiringException {
        for (int a = 0; a < handlers.size(); a++) {
            Object handler = handlers.get(a);
            if (handler instanceof ExtensibleHandler) {
                final ExtensibleHandler extensibleHandler = (ExtensibleHandler) handler;
                final List<Class<?>> markerInterfaces = extensibleHandler.getMarkerInterfaces();
                for (int i = 0; i < markerInterfaces.size(); i++) {
                    final Class<?> markerInterface = markerInterfaces.get(i);
                    final List<?> extensions = getHandlers(markerInterface);
                    // ok now time for try the wiring
                    extensibleHandler.wireExtensions(markerInterface, extensions);

                }
            }
        }
    }
    
    private void registerHandlersPackage(HandlersPackage handlersPackage, HierarchicalConfiguration handlerConfig, List<HierarchicalConfiguration> children) {
        List<String> c = handlersPackage.getHandlers();

        for (Iterator<String> i = c.iterator(); i.hasNext();) {
            String cName = i.next();

            try {
                CombinedConfiguration conf = new CombinedConfiguration();
                HierarchicalConfiguration cmdConf = addHandler(cName);
                conf.addConfiguration(cmdConf);
                if (handlerConfig != null) {
                    conf.addConfiguration(handlerConfig);
                }
                children.add(conf);
            } catch (ConfigurationException e) {
                throw new FatalBeanException("Unable to create configuration for handler " + cName, e);
            }
        }
    }

    /**
     * Return a DefaultConfiguration build on the given command name and
     * classname.
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
     * 
     * @see
     * org.apache.james.protocols.api.ProtocolHandlerChain#getHandlers(java.
     * lang.Class)
     */
    public <T> LinkedList<T> getHandlers(Class<T> type) {
        LinkedList<T> classHandlers = new LinkedList<T>();
        for (int i = 0; i < handlers.size(); i++) {
            if (type.isInstance(handlers.get(i))) {
                classHandlers.add((T)handlers.get(i));
            }
            
        }

        return classHandlers;
    }


    /*
     * 
     */
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }
}
