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

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.phoenix.tools.configuration.ConfigurationBuilder;
import org.apache.james.container.spring.adaptor.ConfigurationProvider;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class AvalonBeanDefinitionReader extends AbstractBeanDefinitionReader {

    private ConfigurationProvider configurationProvider;
    private ResourceLoader resourceLoader;
    private ClassLoader beanClassLoader;

    public AvalonBeanDefinitionReader(BeanDefinitionRegistry beanDefinitionRegistry) {
        super(beanDefinitionRegistry);
    }

    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
        Configuration configuration = null;
        try {
            configuration = getConfiguration(resource);
        } catch (IOException e) {
            throw new BeanDefinitionStoreException("could not read input resource", e);
        }

        configuration.getChildren("assembly");
        if (configuration == null) return 0;

        Configuration[] blocks = configuration.getChildren("block");
        if (blocks == null) return 0;

        for (int i = 0; i < blocks.length; i++) {
            Configuration block = blocks[i];
            BeanDefinitionHolder definitionHolder = loadBeanDefinition(block);
            getBeanFactory().registerBeanDefinition(definitionHolder.getBeanName(), definitionHolder.getBeanDefinition());
        }

        return blocks.length;
    }

    private BeanDefinitionHolder loadBeanDefinition(Configuration block) throws BeanDefinitionStoreException {
        String name = null;
        String className = null;
        try {
            name = block.getAttribute("name");
        } catch (ConfigurationException e) {
            throw new BeanDefinitionStoreException("avalon assembly block mandatory name attribute missing", e);
        }
        try {
            className = block.getAttribute("class");
        } catch (ConfigurationException e) {
            throw new BeanDefinitionStoreException("avalon assembly block mandatory class attribute missing", e);
        }

        
        AvalonBeanDefinition beanDefinition = null;
        try {
            beanDefinition = createBeanDefinition(className);
        } catch (ClassNotFoundException e) {
            throw new BeanDefinitionStoreException("bean class not found", e);
        }

        beanDefinition.addAllServiceReferences(loadServiceReferences(block));        

        return new BeanDefinitionHolder(beanDefinition, name);
    }

    public AvalonBeanDefinition createBeanDefinition(String className) throws ClassNotFoundException {
        AvalonBeanDefinition bd = new AvalonBeanDefinition();
        if (className != null) {
            if (getBeanClassLoader() != null) {
                bd.setBeanClass(ClassUtils.forName(className, getBeanClassLoader()));
            }
            else {
                bd.setBeanClassName(className);
            }
        }
        return bd;
    }
    
    private List loadServiceReferences(Configuration block) {
        List serviceReferences = new ArrayList();
        Configuration[] referencedComponentDefs  = block.getChildren("provide");
        if (referencedComponentDefs == null) return serviceReferences;

        String name = null;
        String roleClassname = null;
        for (int i = 0; i < referencedComponentDefs.length; i++) {
            Configuration referencedComponentDef = referencedComponentDefs[i];

            try {
                name = referencedComponentDef.getAttribute("name");
            } catch (ConfigurationException e) {
                throw new BeanDefinitionStoreException("avalon assembly provide mandatory name attribute missing", e);
            }
            try {
                roleClassname = referencedComponentDef.getAttribute("role");
            } catch (ConfigurationException e) {
                throw new BeanDefinitionStoreException("avalon assembly provide mandatory role attribute missing", e);
            }
            AvalonServiceReference avalonServiceReference = new AvalonServiceReference(name, roleClassname);
            serviceReferences.add(avalonServiceReference);
        }
        return serviceReferences;
    }

    private Configuration getConfiguration(Resource resource) throws IOException {
        InputSource inputSource = new InputSource(resource.getInputStream());
        try
        {
            Configuration configuration = ConfigurationBuilder.build(inputSource, null, null);
            return configuration;
        }
        catch( final Exception e )
        {
            throw new RuntimeException("failed loading configuration ", e);
        }
    }
}
