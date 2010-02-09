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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.james.container.spring.Registry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Load Configuration and act as provider
 * 
 *
 */
public class SpringConfigurationRegistry implements Registry<HierarchicalConfiguration>, ResourceLoaderAware, InitializingBean {

	private ResourceLoader loader;
	private Map<String,HierarchicalConfiguration> confMap = new HashMap<String,HierarchicalConfiguration>();
    private Map<String,String> resources;

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.container.spring.Registry#getForComponent(java.lang.String)
	 */
	public HierarchicalConfiguration getForComponent(String name)
			throws RegistryException {
	    HierarchicalConfiguration conf = confMap.get(name);
	    if (conf != null) {
	        return conf;
	    } else {
	        Resource r = loader.getResource("classpath:" + name + ".xml");
	        if (r.exists()) {
	            try {
                    return getConfig(r);
                } catch (Exception e) {
                    throw new RegistryException("Unable to load configuration for component " + name,e);                    
                }
	        }
	    }
	    throw new RegistryException("Unable to load configuration for component " + name);
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

    public void setConfigurationMappings(Map<String,String> resources) {
        this.resources = resources;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.container.spring.Registry#registerForComponent(java.lang.String, java.lang.Object)
     */
    public void registerForComponent(String name, HierarchicalConfiguration conf) {
        confMap.put(name, conf);        
    }


    /*
     * 
     */
    public void afterPropertiesSet() throws Exception {
        if (resources != null) {
            Iterator<String> it = resources.keySet().iterator();

            while (it.hasNext()) {
                String key = it.next();
                String value = resources.get(key);
                confMap.put(key,getForComponent(value));
            }
        }
    }

}
