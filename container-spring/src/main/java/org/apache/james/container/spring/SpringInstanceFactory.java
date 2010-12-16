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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.resolver.api.InstanceFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * {@link InstanceFactory} implementation which use a {@link BeanFactory} to handle the loading / injection of resources
 * 
 *
 */
public class SpringInstanceFactory implements InstanceFactory, BeanFactoryAware{
    private ConfigurableListableBeanFactory cFactory;
  


    /*
     * (non-Javadoc)
     * @see org.apache.james.services.InstanceFactory#newInstance(java.lang.Class)
     */
    public <T> T newInstance(Class<T> clazz) throws InstanceException {
        return newInstance(clazz, null, null);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.services.InstanceFactory#newInstance(java.lang.Class, org.apache.commons.logging.Log, org.apache.commons.configuration.HierarchicalConfiguration)
     */
    @SuppressWarnings("unchecked")
    public <T> T newInstance(Class<T> clazz, Log log, HierarchicalConfiguration config) throws InstanceException {
        try {
            Object obj = clazz.newInstance();
            if (log != null) {
                if (obj instanceof LogEnabled) {
                    ((LogEnabled) obj).setLog(log);
                }
            }
            if (config != null) {
                if (obj instanceof Configurable) {
                    try {
                        ((Configurable) obj).configure(config);
                    } catch (ConfigurationException e) {
                        throw new InstanceException("Unable to config " + obj);
                    }
                } 
            }
            cFactory.autowireBean(obj);
            return (T)cFactory.initializeBean(obj, obj.toString());
            
        } catch (InstantiationException e) {
            throw new InstanceException("Unable to instance " + clazz , e);
        } catch (IllegalAccessException e) {
            throw new InstanceException("Unable to instance " + clazz , e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
     */
    public void setBeanFactory(BeanFactory factory) throws BeansException {
        cFactory = (ConfigurableListableBeanFactory) factory;
    }

}
