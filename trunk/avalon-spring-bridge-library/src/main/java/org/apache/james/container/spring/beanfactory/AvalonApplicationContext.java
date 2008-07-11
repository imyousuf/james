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
package org.apache.james.container.spring.beanfactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * loads an Avalon/Phoenix configuration.
 * this is done by using a two step approach:
 * 1. loading the avalon mocking beans from a spring xml beans configuration
 * 2. loading the James beans from a james-assembly.xml 
 */
public class AvalonApplicationContext extends AbstractRefreshableApplicationContext {

    public static final Resource RESOURCE_SPRING_BEANS = new ClassPathResource("spring-beans.xml");
    public static final Resource RESOURCE_JAMES_ASSEMBLY = new ClassPathResource("james-assembly.xml");
    
    private Resource containerConfigurationResource;
    private Resource applicationConfigurationResource;

    /**
     * configuration-by-convention constructor, tries to find default config files on classpath
     */
    public AvalonApplicationContext() {
        this(RESOURCE_SPRING_BEANS, RESOURCE_JAMES_ASSEMBLY);
    }
    
    
    public AvalonApplicationContext(Resource containerConfigurationResource,
                                    Resource applicationConfigurationResource) {
        this(null, containerConfigurationResource, applicationConfigurationResource);
    }

    public AvalonApplicationContext(ApplicationContext parent, 
                                    Resource containerConfigurationResource, 
                                    Resource applicationConfigurationResource) {
        super(parent);
        this.containerConfigurationResource = containerConfigurationResource;
        this.applicationConfigurationResource = applicationConfigurationResource;
        refresh();
    }

    protected void loadBeanDefinitions(DefaultListableBeanFactory defaultListableBeanFactory) throws IOException, BeansException {
        loadAvalonBasedBeanDefinitions(defaultListableBeanFactory, containerConfigurationResource, applicationConfigurationResource);
    }

    public static void loadAvalonBasedBeanDefinitions(DefaultListableBeanFactory defaultListableBeanFactory, Resource containerConfigurationResource, Resource applicationConfigurationResource) {
        XmlBeanDefinitionReader containerBeanDefinitionReader = new XmlBeanDefinitionReader(defaultListableBeanFactory);
        int containerBeanCount = containerBeanDefinitionReader.loadBeanDefinitions(containerConfigurationResource);

        AvalonBeanDefinitionReader applicationBeanDefinitionReader = new AvalonBeanDefinitionReader(defaultListableBeanFactory);
        int applicationBeanCount = applicationBeanDefinitionReader.loadBeanDefinitions(applicationConfigurationResource);

        int totalBeanCount = containerBeanCount + applicationBeanCount;
    }
}
