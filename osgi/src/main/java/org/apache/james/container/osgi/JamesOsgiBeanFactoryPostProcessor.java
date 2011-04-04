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
package org.apache.james.container.osgi;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.james.container.spring.bean.postprocessor.ConfigurableBeanPostProcessor;
import org.apache.james.container.spring.bean.postprocessor.LogEnabledBeanPostProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.osgi.extender.OsgiBeanFactoryPostProcessor;

/**
 * {@link OsgiBeanFactoryPostProcessor} which add all life-cycle needed
 * {@link BeanPostProcessor} to the {@link ConfigurableListableBeanFactory} on
 * creation. This ensures that the right methods are called during startup when
 * deploying James bundles.
 * 
 * Beside this it also add support for JSR-250 annotations
 */
public class JamesOsgiBeanFactoryPostProcessor implements OsgiBeanFactoryPostProcessor {

    private String confDir;

    public void setConfigurationDirectory(String confDir) {
        this.confDir = confDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.osgi.extender.OsgiBeanFactoryPostProcessor#
     * postProcessBeanFactory(org.osgi.framework.BundleContext,
     * org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
     */
    public void postProcessBeanFactory(BundleContext context, ConfigurableListableBeanFactory factory) throws BeansException, InvalidSyntaxException, BundleException {

        // life-cycle for LogEnabled
        OsgiLogProvider logProvider = new OsgiLogProvider();
        LogEnabledBeanPostProcessor logProcessor = new LogEnabledBeanPostProcessor();
        logProcessor.setLogProvider(logProvider);
        logProcessor.setBeanFactory(factory);
        factory.addBeanPostProcessor(logProcessor);

        // Life-cycle for Configurable
        OsgiConfigurationProvider confProvider = new OsgiConfigurationProvider(confDir);
        ConfigurableBeanPostProcessor confProcessor = new ConfigurableBeanPostProcessor();
        confProcessor.setConfigurationProvider(confProvider);
        confProcessor.setBeanFactory(factory);
        factory.addBeanPostProcessor(confProcessor);

        // Support for JSR-250 annotations
        CommonAnnotationBeanPostProcessor commAnnotationProcessor = new CommonAnnotationBeanPostProcessor();
        commAnnotationProcessor.setBeanFactory(factory);
        commAnnotationProcessor.setInitAnnotationType(PostConstruct.class);
        commAnnotationProcessor.setDestroyAnnotationType(PreDestroy.class);
        commAnnotationProcessor.setResourceFactory(factory);
        factory.addBeanPostProcessor(commAnnotationProcessor);

    }
}
