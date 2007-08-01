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

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.container.spring.adaptor.DefaultServiceManagerFactory;
import org.apache.james.container.spring.adaptor.ServiceManagerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * calls service() for all avalon components
 */
public class ServicePropagator extends AbstractPropagator implements BeanPostProcessor, Ordered {

    private ServiceManagerFactory serviceManagerFactory;
    
    public void setServiceManagerFactory(DefaultServiceManagerFactory serviceManagerFactory) {
        this.serviceManagerFactory = serviceManagerFactory;
    }
    
    protected Class getLifecycleInterface() {
        return Serviceable.class;
    }

    protected void invokeLifecycleWorker(String beanName, Object bean, BeanDefinition beanDefinition) {
        if (!(bean instanceof Serviceable)) return;
        Serviceable serviceable = (Serviceable) bean;
        try {
            ServiceManager serviceManager = serviceManagerFactory.getInstanceFor(beanName, beanDefinition);
            if (serviceManager == null) {
                throw new RuntimeException("failed to create service manager for " + beanName);
            }
            serviceable.service(serviceManager);
        } catch (ServiceException e) {
            throw new RuntimeException("could not successfully run service method on component of type " + serviceable.getClass(), e);
        } catch (Exception e) {
            throw new RuntimeException("could not successfully run service method on component of type " + serviceable.getClass(), e);
        }
    }

    public int getOrder() {
        return 2;
    }
}
