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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;

/**
 * LoaderService which try to lookup instances of classes in the ApplicationContext of Spring.
 * If no such bean exists it create it on the fly and add it toe the ApplicationContext
 * 
 *
 */
@SuppressWarnings("unused")
public class JSR250LoaderService extends CommonAnnotationBeanPostProcessor implements LoaderService {

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.api.kernel.LoaderService#injectDependencies(java.lang.Object)
	 */
	public void injectDependencies(Object obj) {
		postProcessAfterInitialization(obj, obj.getClass().getName());
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.api.kernel.LoaderService#injectDependenciesWithLifecycle(java.lang.Object, org.apache.commons.logging.Log, org.apache.commons.configuration.HierarchicalConfiguration)
	 */
	public void injectDependenciesWithLifecycle(Object obj, Log logger,
			HierarchicalConfiguration config) {
		if (obj instanceof LogEnabled) {
			((LogEnabled) obj).setLog(logger);
		}
		if (obj instanceof Configurable) {
			try {
			((Configurable) obj).configure(config);
			} catch (ConfigurationException ex) {
				throw new RuntimeException("Unable to configure object " + obj, ex);
			}
		}
		injectDependencies(obj);
	}
}
