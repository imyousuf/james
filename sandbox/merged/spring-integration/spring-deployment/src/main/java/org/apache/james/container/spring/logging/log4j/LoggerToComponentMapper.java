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
package org.apache.james.container.spring.logging.log4j;

import org.apache.avalon.framework.logger.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.Resource;

import java.util.Properties;
import java.io.IOException;
import java.net.URL;

/**
 * simple default implementation. change the log4j configuration file to change specific logging behavior
 */
public class LoggerToComponentMapper implements org.apache.james.container.spring.logging.LoggerToComponentMapper {
    private Resource propertiesResource;

    public LoggerToComponentMapper() {
    }

    public void setConfigurationResource(Resource propertiesResource) {
        this.propertiesResource = propertiesResource;    
    }
    
    public void init() {
        Properties properties;
        URL url = null;
        try {
            url = propertiesResource.getURL();
            properties = PropertiesLoaderUtils.loadProperties(propertiesResource);
        } catch (IOException e) {
            throw new RuntimeException("failed to load log4j properties from " + url);
        }
        PropertyConfigurator.configure(properties);
    } 
    
    public Logger getComponentLogger(String beanName) {
        return new AvalonToLog4jLogger(org.apache.log4j.Logger.getLogger(beanName));
    }
}
