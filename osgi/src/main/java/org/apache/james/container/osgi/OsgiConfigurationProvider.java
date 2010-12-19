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
package org.apache.james.container.osgi;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.james.container.spring.lifecycle.api.ConfigurationProvider;

/**
 * Load configuration files from the specified configuration directory
 * 
 *
 */
public class OsgiConfigurationProvider implements ConfigurationProvider{

    private String configDir;

    public OsgiConfigurationProvider(String configDir) {
        this.configDir = configDir;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.container.spring.lifecycle.ConfigurationProvider#getConfiguration(java.lang.String)
     */
    public HierarchicalConfiguration getConfiguration(String beanName) throws ConfigurationException {
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        
        config.load(new File(configDir +"/" + beanName + ".xml"));        
        return config;
        
    }

    public void registerConfiguration(String beanName, HierarchicalConfiguration conf) {
        // TODO Auto-generated method stub
        
    }

}
