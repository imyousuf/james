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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.Collection;

/**
 * visitor. iterating over all spring beans having some specific implementation 
 */
public abstract class AbstractPropagator implements BeanFactoryAware {

    private Collection excludeBeans;
    private BeanFactory beanFactory;

    
    /**
     * Gets the bean factory
     * @return the beanFactory not null
     */
    public final BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
    
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    protected BeanDefinition getBeanDefinition(String beanName) {
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            ConfigurableListableBeanFactory configurableListableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
            return configurableListableBeanFactory.getBeanDefinition(beanName); 
        }
        return null; // cannot lookup bean definition
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (excludeBeans == null || !excludeBeans.contains(beanName)) {
            BeanDefinition beanDefinition = getBeanDefinition(beanName);
            invokeLifecycleWorker(beanName, bean, beanDefinition);
        }
        return bean;
    }

    public void setExcludeBeans(Collection excludeBeans) {
    	this.excludeBeans=excludeBeans;
    }

    protected abstract Class getLifecycleInterface();

    protected abstract void invokeLifecycleWorker(String beanName, Object bean, BeanDefinition beanDefinition);

}
