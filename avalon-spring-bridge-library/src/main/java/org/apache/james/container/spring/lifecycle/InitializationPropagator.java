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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.api.kernel.LoaderService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * calls initialize() for all avalon components
 */
public class InitializationPropagator extends AbstractPropagator implements BeanPostProcessor, Ordered, LoaderService {

    protected Class<?> getLifecycleInterface() {
        return Initializable.class;
    }

    protected void invokeLifecycleWorker(String beanName, Object bean, BeanDefinition beanDefinition) {
        // TODO: share reflection code
        Method[] methods = injectResources(bean);
        try {
            ContainerUtil.initialize(bean);;
            postConstruct(bean, methods);
        } catch (Exception e) {
            throw new RuntimeException("could not initialize component of type " + bean.getClass(), e);
        }
    }

    private void postConstruct(Object bean, Method[] methods)
            throws IllegalAccessException, InvocationTargetException {
        for (Method method : methods) {
            PostConstruct postConstructAnnotation = method.getAnnotation(PostConstruct.class);
            if (postConstructAnnotation != null) {
                Object[] args = {};
                method.invoke(bean, args);
            }
        }
    }

    private Method[] injectResources(Object bean) {
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            Resource resourceAnnotation = method.getAnnotation(Resource.class);
            if (resourceAnnotation != null) {
                final String name = resourceAnnotation.name();
                final Object service;
                if ("org.apache.james.LoaderService".equals(name)) {
                    service = this;
                } else {
                    service = get(name);
                }
                
                if (bean == null) {
               } else {
                    try {
                        Object[] args = {service};
                        method.invoke(bean, args);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Injection failed", e);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Injection failed", e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("Injection failed", e);
                    }
                }
            }
        }
        return methods;
    }

    public int getOrder() {
        return 4;
    }

    public Object get(String name) {
        return getBeanFactory().getBean(name);
    }
    
    public <T> T load(Class<T> type) {
        try {
            // TODO: Use Guice to load type
            final T newInstance = type.newInstance();
            injectResources(newInstance);
            return newInstance;
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } 
    }
}
