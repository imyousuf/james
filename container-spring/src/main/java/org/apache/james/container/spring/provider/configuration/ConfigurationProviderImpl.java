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
package org.apache.james.container.spring.provider.configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Load Configuration and act as provider
 * 
 *
 */
public class ConfigurationProviderImpl implements ConfigurationProvider, ResourceLoaderAware, InitializingBean {

    private ResourceLoader loader;
    private Map<String, HierarchicalConfiguration> confMap = new HashMap<String, HierarchicalConfiguration>();
    private Map<String, String> resources;

    /**
     * Return the configuration prefix to load the config. In this case its
     * file://conf/
     * 
     * @return prefix
     */
    protected String getConfigPrefix() {
        return "file://conf/";
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
    
    private XMLConfiguration getConfig(Resource r) throws ConfigurationException, IOException {
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        
        // Use InputStream so we are not bound to File implementations of the config
        config.load(r.getInputStream());
        return config;
    }

    public void setConfigurationMappings(Map<String,String> resources) {
        this.resources = resources;
    }


    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (resources != null) {
            Iterator<String> it = resources.keySet().iterator();

            while (it.hasNext()) {
                String key = it.next();
                String value = resources.get(key);
                registerConfiguration(key,getConfiguration(value));
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.container.spring.lifecycle.ConfigurationProvider#getConfiguration(java.lang.String)
     */
    public HierarchicalConfiguration getConfiguration(String name) throws ConfigurationException {
        HierarchicalConfiguration conf = confMap.get(name);
        if (conf != null) {
            return conf;
        } else {
            
            int i = name.indexOf(".");
            String configName;
            String configPart = null;
            
            if (i > -1) {
                configPart = name.substring(i+1);
                configName = name.substring(0, i);
            } else {
                configName = name;
            }

            Resource r = loader.getResource(getConfigPrefix()+ configName + ".xml");
            if (r.exists()) {
                try {
                    HierarchicalConfiguration config = getConfig(r);
                    if (configPart != null) {
                        return config.configurationAt(configPart);
                    } else {
                        return config;
                    }
                    
                } catch (Exception e) {
                    throw new ConfigurationException("Unable to load configuration for component " + name, e);                    
                }
            }
        }
        throw new ConfigurationException("Unable to load configuration for component " + name);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.container.spring.lifecycle.ConfigurationProvider#registerConfiguration(java.lang.String, org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void registerConfiguration(String beanName, HierarchicalConfiguration conf) {
        confMap.put(beanName,conf);
    }

}
