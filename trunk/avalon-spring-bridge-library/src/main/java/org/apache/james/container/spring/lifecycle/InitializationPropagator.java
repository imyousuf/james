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

import java.lang.reflect.Method;

import javax.annotation.PostConstruct;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * calls initialize() for all avalon components
 */
public class InitializationPropagator extends AbstractPropagator implements BeanPostProcessor, Ordered {

    protected Class getLifecycleInterface() {
        return Initializable.class;
    }

    protected void invokeLifecycleWorker(String beanName, Object bean, BeanDefinition beanDefinition) {
        try {
            ContainerUtil.initialize(bean);
            Method[] methods = bean.getClass().getMethods();
            for (Method method : methods) {
                PostConstruct postConstructAnnotation = method.getAnnotation(PostConstruct.class);
                if (postConstructAnnotation != null) {
                    Object[] args = {};
                    method.invoke(bean, args);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("could not initialize component of type " + bean.getClass(), e);
        }
    }

    public int getOrder() {
        return 4;
    }

}
