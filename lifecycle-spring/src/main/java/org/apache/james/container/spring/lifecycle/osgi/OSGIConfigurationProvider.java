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
package org.apache.james.container.spring.lifecycle.osgi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

public class OSGIConfigurationProvider implements org.apache.james.container.spring.lifecycle.ConfigurationProvider{

    @Override
    public void registerConfiguration(String beanName, HierarchicalConfiguration conf) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public HierarchicalConfiguration getConfiguration(String beanName) throws ConfigurationException {
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        
        // Don't split attributes which can have bad side-effects with matcher-conditions.
        // See JAMES-1233
        config.setAttributeSplittingDisabled(true);
        
        // Use InputStream so we are not bound to File implementations of the
        // config
        try {
            config.load(new FileInputStream("/tmp/" + beanName + ".xml"));
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Bean " + beanName);
        }
        
        return config;
    }

}
