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
package org.apache.james.container.spring.osgi;

import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.container.spring.lifecycle.ConfigurationProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.service.exporter.support.BeanNameServicePropertiesResolver;

/**
 * This {@link BundleListener} use the extender pattern to scan all loaded
 * bundles if a class name with a given name is present. If so it register in
 * the {@link BeanDefinitionRegistry} and also register it to
 * {@link BundleContext} as service. This allows to dynamic load and unload OSGI
 * bundles
 * 
 */
public abstract class AbstractServiceTracker implements BeanFactoryAware, BundleListener, BundleContextAware, InitializingBean, DisposableBean {

    private BundleContext context;
    private BeanFactory factory;
    private String configuredClass;
    private BeanNameServicePropertiesResolver resolver;
    private ServiceRegistration reg;

    @Override
    public void setBeanFactory(BeanFactory factory) throws BeansException {
        this.factory = factory;
    }

    @Override
    public void setBundleContext(BundleContext context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void bundleChanged(BundleEvent event) {
        Bundle b = event.getBundle();

        // Check if the event was fired for this class
        if (b.equals(this.context.getBundle())) {
            return;
        }

        switch (event.getType()) {
        case BundleEvent.STARTED:
            Enumeration<?> entrs = b.findEntries("/", "*.class", true);
            if (entrs != null) {

                // Loop over all the classes 
                while (entrs.hasMoreElements()) {
                    URL e = (URL) entrs.nextElement();
                    String file = e.getFile();

                    String className = file.replaceAll("/", ".").replaceAll(".class", "").replaceFirst(".", "");
                    if (className.equals(configuredClass)) {
                        
                        // Get the right service properties from the resolver
                        Properties p = new Properties();
                        p.putAll(resolver.getServiceProperties(getComponentName()));
                        Class<?> clazz = getServiceClass();
                        
                        // Create the definition and register it
                        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
                        BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(className).getBeanDefinition();
                        registry.registerBeanDefinition(getComponentName(), def);

                        // register the bean as service in the BundleContext
                        reg = b.getBundleContext().registerService(clazz.getName(), factory.getBean(getComponentName(), clazz), p);
                    }
                }
            }
            break;
        case BundleEvent.STOPPED:
            if (reg != null) {

                // Check if we need to unregister the service
                if (b.equals(reg.getReference().getBundle())) {
                    reg.unregister();
                }
            }
            break;
        default:
            break;
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ConfigurationProvider confProvider = factory.getBean(ConfigurationProvider.class);
        HierarchicalConfiguration config = confProvider.getConfiguration(getComponentName());

        // Setup a resolver
        resolver = new BeanNameServicePropertiesResolver();
        resolver.setBundleContext(context);

        // Get the configuration for the class
        configuredClass = config.getString("[@class]");
        context.addBundleListener(this);
    }

    @Override
    public void destroy() throws Exception {
        // Its time to unregister the listener so we are sure resources are released
        if (context != null) {
            context.removeBundleListener(this);
        }
    }

    /**
     * Return the name of the component
     * 
     * @return name
     */
    protected abstract String getComponentName();

    /**
     * Return the class which will be used to expose the service in the OSGI
     * registry
     * 
     * @return sClass
     */
    protected abstract Class<?> getServiceClass();

}
