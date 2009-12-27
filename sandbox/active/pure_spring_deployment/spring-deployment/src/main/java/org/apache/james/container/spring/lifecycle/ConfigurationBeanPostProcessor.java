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

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.james.lifecycle.Configurable;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class ConfigurationBeanPostProcessor extends
		AbstractLifeCycleBeanPostProcessor<Configurable> implements
		ResourceLoaderAware {

	private XMLConfiguration config;
	private String configFile;
	private ResourceLoader loader;

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	public void init() {
		Resource resource = loader.getResource(configFile);
		if (!resource.exists()) {
			throw new RuntimeException("could not locate configuration file "
					+ configFile);
		}
		try {
			config = new XMLConfiguration();
			config.setDelimiterParsingDisabled(true);
			config.load(resource.getFile());
		} catch (Exception e1) {
			throw new RuntimeException("could not open configuration file "
					+ configFile, e1);
		}
	}

	@Override
	protected void executeLifecycleMethod(Configurable bean, String beanname,
			String lifecyclename) throws Exception {
		HierarchicalConfiguration beanConfig = config.configurationAt(lifecyclename);
		bean.configure(beanConfig);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public int getOrder() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.context.ResourceLoaderAware#setResourceLoader(org
	 * .springframework.core.io.ResourceLoader)
	 */
	public void setResourceLoader(ResourceLoader loader) {
		this.loader = loader;
	}

	@Override
	protected Class<Configurable> getLifeCycleInterface() {
		return Configurable.class;
	}

}
