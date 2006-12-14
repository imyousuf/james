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
package org.apache.james.container.spring.processor;

import org.apache.avalon.framework.activity.Initializable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * calls initialize() for all avalon components
 */
public class InitializationProcessor extends AbstractProcessor implements BeanPostProcessor, Ordered {

    public int getOrder() {
        return 4;
    }

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Initializable && isIncluded(beanName)) {
	        Initializable initializable = (Initializable) bean;
	        try {
	            initializable.initialize();
	        } catch (Exception e) {
	            throw new RuntimeException("could not initialize component of type " + initializable.getClass(), e);
	        }			
		}
		return bean;
	}

}
