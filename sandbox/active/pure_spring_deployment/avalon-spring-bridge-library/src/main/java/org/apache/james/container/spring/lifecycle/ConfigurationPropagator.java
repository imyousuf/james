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
package org.apache.james.container.spring.lifecycle;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.container.spring.adaptor.ConfigurationProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * calls configure() for all avalon components
 */
public class ConfigurationPropagator extends AbstractPropagator implements BeanPostProcessor, Ordered {

    private Configuration configuration;

    public void setConfigurationProvider(ConfigurationProvider configurationProvider) {
        this.configuration = configurationProvider.getConfiguration();
    }

    private boolean isConfigurationEmpty(Configuration componentConfiguration) {
        return     (componentConfiguration.getChildren() == null || componentConfiguration.getChildren().length == 0)
                && (componentConfiguration.getAttributeNames() == null || componentConfiguration.getAttributeNames().length == 0);
    }

    public int getOrder() {
        return 3;
    }

    protected Class<?> getLifecycleInterface() {
        return Configurable.class;
    }

    protected void invokeLifecycleWorker(String beanName, Object bean, BeanDefinition beanDefinition) {
        if (!(bean instanceof Configurable)) return;
        Configurable configurable = (Configurable)bean;
         try {
             Configuration componentConfiguration = configuration.getChild(beanName);
             if (isConfigurationEmpty(componentConfiguration)) {
                 // heuristic: try lowercase
                 componentConfiguration = configuration.getChild(beanName.toLowerCase());
             }
             if (isConfigurationEmpty(componentConfiguration)) {
                 System.out.println("configuraton empty for bean " + beanName);
             }
             configurable.configure(componentConfiguration);
         } catch (ConfigurationException e) {
             throw new RuntimeException("could not configure component of type " + configurable.getClass(), e);
         }
//         catch (Exception e) {
//             throw new RuntimeException("could not configure component of type " + serviceable.getClass(), e);
//         }
     }
}
