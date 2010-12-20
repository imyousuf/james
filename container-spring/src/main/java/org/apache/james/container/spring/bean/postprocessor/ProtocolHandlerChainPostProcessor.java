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
package org.apache.james.container.spring.bean.postprocessor;

import java.util.List;

import org.apache.james.container.spring.bean.factorypostprocessor.ProtocolHandlerChainFactoryPostProcessor;
import org.apache.james.protocols.api.ExtensibleHandler;
import org.apache.james.protocols.api.WiringException;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class ProtocolHandlerChainPostProcessor extends ProtocolHandlerChainFactoryPostProcessor implements BeanPostProcessor {

    /**
     * Check if the bean was registered within the instance and if so see if it is an {@link ExtensibleHandler} implementation
     * 
     * If thats the case it will do all the needed wiring 
     */
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        
        // check if ths instance is responsible for the bean and if so if the bean is an instance of ExtensibleHandler
        if (getHandlers().contains(beanName) && bean instanceof ExtensibleHandler) {
            final ExtensibleHandler extensibleHandler = (ExtensibleHandler) bean;
            final List<Class<?>> markerInterfaces = extensibleHandler.getMarkerInterfaces();
            for (int i = 0; i < markerInterfaces.size(); i++) {
                final Class<?> markerInterface = markerInterfaces.get(i);
                final List<?> extensions = getHandlers(markerInterface);
                try {
                    // ok now time for try the wiring
                    extensibleHandler.wireExtensions(markerInterface, extensions);
                } catch (WiringException e) {
                    throw new FatalBeanException("Unable to wire the handler " + bean + " with name " + beanName, e);
                }
            }
        }
        
        return bean;
    }

    /**
     * Nothing todo so just return the bean
     */
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
