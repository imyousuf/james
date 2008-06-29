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
package org.apache.james.container.spring.adaptor;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.phoenix.tools.configuration.ConfigurationBuilder;
import org.apache.james.container.spring.configuration.ConfigurationInterceptor;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

/**
 * loads the well-known classic James configuration file
 *
  * TODO make this thing be based on Resource class and inject resource.getInputStream() into InputSource 
 */
public class AvalonConfigurationFileProvider implements ConfigurationProvider, ResourceLoaderAware {

    private List configurationInterceptors;
    private String configuration;
    private ResourceLoader resourceLoader;
    
    public Configuration getConfiguration() {
        InputStream inputStream = null;
        String systemId = null;
        
        Resource resource = resourceLoader.getResource(configuration);
        if (!resource.exists()) {
            throw new RuntimeException("could not locate configuration file " + configuration);
        }
        try {
            inputStream = resource.getInputStream();
            systemId = resource.getURL().toString();
        } catch (IOException e1) {
            throw new RuntimeException("could not open configuration file " + configuration, e1);
        }
        InputSource inputSource = new InputSource(inputStream);
        Configuration configuration;
        try
        {
            inputSource.setSystemId(systemId);
            configuration = ConfigurationBuilder.build(inputSource, null, null);
        }
        catch( final Exception e )
        {
//            getLogger().error( message, e );
            throw new RuntimeException("failed loading configuration ", e);
        }

        // apply all interceptors
        if (configuration != null && configurationInterceptors != null) {
            Iterator interceptorsIterator = configurationInterceptors.iterator();
            while (interceptorsIterator.hasNext()) {
                ConfigurationInterceptor configurationInterceptor = (ConfigurationInterceptor) interceptorsIterator.next();
                configuration = configurationInterceptor.intercept(configuration);
            }
        }
        
        return configuration;
    }
    public void setConfigurationResource(String configuration) {
        this.configuration = configuration;
    }

    public void setConfigurationInterceptors(List configurationInterceptors) {
        this.configurationInterceptors = configurationInterceptors;
    }

    public synchronized void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
