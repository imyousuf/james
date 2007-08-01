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
package org.apache.james.container.spring.adaptor;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.container.spring.beanfactory.AvalonBeanDefinition;
import org.apache.james.container.spring.beanfactory.AvalonServiceReference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * provides a Avalon-style service manager to all James components
 */
public class DefaultServiceManagerFactory implements ApplicationContextAware, ServiceManagerFactory {

	private ApplicationContext applicationContext;

    private class ServiceManagerBridge implements ServiceManager {

		private final Map avalonServices = new HashMap();
		
        public ServiceManagerBridge(List avalonServiceReferences) {
            populateServiceMap(avalonServiceReferences);
        }

        private void populateServiceMap(List avalonServiceReferences) {
            Iterator iterator = avalonServiceReferences.iterator();
            while (iterator.hasNext()) {
                AvalonServiceReference serviceReference = (AvalonServiceReference) iterator.next();
                String name = serviceReference.getName();
                String rolename = serviceReference.getRolename();
                
                // the interface to be injected 
                Class roleClass = null;
                try {
                    roleClass = Class.forName(rolename);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("cannot load class for role " + roleClass, e);
                }
                // the object to be injected (reduced to roleClass)
                Object injectionCandidate = applicationContext.getBean(name);
                if (!roleClass.isInstance(injectionCandidate)) throw new RuntimeException("cannot assign object as role as specified");

                if (avalonServices.containsKey(rolename)) {
                    throw new IllegalStateException("avalon service references role name not unique: " + rolename);
                }
                avalonServices.put(rolename, injectionCandidate);
            }
        }

        public Object lookup(String componentIdentifier)
				throws ServiceException {
            Object service = avalonServices.get(componentIdentifier);
            return service;
        }

		public boolean hasService(String componentIdentifier) {
			try {
				return null != lookup(componentIdentifier);
			} catch (ServiceException e) {
				return false;
			}
		}

		public void release(Object object) {
			throw new IllegalStateException("not yet implemented");
		}
		
	}
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

    public ServiceManager getInstanceFor(String beanName, BeanDefinition beanDefinition) {
        if (beanDefinition == null || !(beanDefinition instanceof AvalonBeanDefinition)) return null;

        AvalonBeanDefinition avalonBeanDefinition = (AvalonBeanDefinition) beanDefinition;
        
        return new DefaultServiceManagerFactory.ServiceManagerBridge(avalonBeanDefinition.getServiceReferences());
	}
	
}