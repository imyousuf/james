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

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.james.container.spring.ConfigurationProvider;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Load Configuration and act as provider
 * 
 *
 */
public class SpringConfigurationProvider implements ConfigurationProvider, ResourceLoaderAware {

	private ResourceLoader loader;

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.container.spring.ConfigurationProvider#getConfigurationForComponent(java.lang.String)
	 */
	public HierarchicalConfiguration getConfigurationForComponent(String name)
			throws ConfigurationException {
	    Resource resource = loader.getResource("classpath:" + name + ".xml");
	    if (resource.exists()) {
	        try {
                return getConfig(resource);
            } catch (IOException e) {
                throw new ConfigurationException("Unable to read config for component " + name, e);
            }
	    }
	    throw new ConfigurationException("Unable to load configuration for component " + name);
	}


	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	public void setResourceLoader(ResourceLoader loader) {
		this.loader = loader;
	}

    
    private XMLConfiguration getConfig(Resource r) throws ConfigurationException, IOException {
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        config.load(r.getFile());
        return config;
    }

}
