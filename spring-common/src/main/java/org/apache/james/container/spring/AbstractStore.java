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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.container.spring.Registry.RegistryException;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Abstract base class which implement Store behavior based on Spring
 *
 */
public abstract class AbstractStore implements BeanFactoryPostProcessor, BeanNameAware, ApplicationContextAware{


    protected String beanName;
    protected ApplicationContext context;
    protected Registry<Log> logProvider;
    protected Registry<HierarchicalConfiguration> confProvider;
    protected Log log;

    protected final List<String> objects = Collections.synchronizedList(new ArrayList<String>());

    public void setConfigurationRegistry(Registry<HierarchicalConfiguration> confProvider) {
        this.confProvider = confProvider;
    }

    public void setLogRegistry(Registry<Log> logProvider) {
        this.logProvider = logProvider;
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang
     * .String)
     */
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.context.ApplicationContextAware#setApplicationContext
     * (org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory arg0) throws BeansException {
        // Store the log object for later usage
        
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) arg0;

        try {
            log = logProvider.getForComponent(beanName);
            List<HierarchicalConfiguration> repConfs = getSubConfigurations(confProvider.getForComponent(beanName));
            ClassLoader theClassLoader = arg0.getBeanClassLoader();
            for (int i = 0; i < repConfs.size(); i++) {
                final HierarchicalConfiguration repConf = repConfs.get(i);
                String repName = repConf.getString("[@name]", null);
                String repClass = repConf.getString("[@class]");

                if (repName == null) {
                    repName = repClass;
                }
                
                if (log.isDebugEnabled()) {
                    log.debug("Starting " + repClass);
                }
                
                Class<?> objectClass = (Class<?>) theClassLoader.loadClass(repClass);
                
                // register configuration and log for the bean
                confProvider.registerForComponent(repName, repConf);
                logProvider.registerForComponent(repName, log);
                
                registry.registerBeanDefinition(repName, BeanDefinitionBuilder.rootBeanDefinition(objectClass).setLazyInit(false).getBeanDefinition());

                objects.add(repName);
                
                if (log.isInfoEnabled()) {
                    StringBuffer logBuffer = new StringBuffer(64).append("Bean  ").append(repName).append(" started.");
                    log.info(logBuffer.toString());
                }
            }
        } catch (RegistryException e) {
            throw new FatalBeanException("Unable to read configuration for " + beanName, e);
        } catch (ClassNotFoundException e) {
            throw new FatalBeanException("Unable to instance class", e);
        }

    }
    
    protected abstract List<HierarchicalConfiguration> getSubConfigurations(HierarchicalConfiguration rootConf);
}
