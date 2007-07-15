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

import java.util.Collection;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.BeansException;

/**
 * visitor. iterating over all spring beans having some specific implementation 
 */
public abstract class AbstractPropagator {

    private Collection excludeBeans;

	public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

        Class lifecycleInterface = getLifecycleInterface();
        String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(configurableListableBeanFactory, lifecycleInterface);
        for (int i = 0; i < beanNames.length; i++) {
            String beanName = beanNames[i];
            if (excludeBeans == null || !excludeBeans.contains(beanName)) {
                BeanDefinition beanDefinition = configurableListableBeanFactory.getBeanDefinition(beanName);
                Object bean = configurableListableBeanFactory.getBean(beanName);
	            invokeLifecycleWorker(beanName, bean, beanDefinition);
            }
        }
    }
    
    public void setExcludeBeans(Collection excludeBeans) {
    	this.excludeBeans=excludeBeans;
    }

    protected abstract Class getLifecycleInterface();

    protected abstract void invokeLifecycleWorker(String beanName, Object bean, BeanDefinition beanDefinition);

}
