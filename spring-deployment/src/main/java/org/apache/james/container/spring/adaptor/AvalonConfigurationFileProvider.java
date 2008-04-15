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
import org.springframework.core.io.Resource;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;
import org.xml.sax.InputSource;

import java.util.Iterator;
import java.util.List;
import java.io.InputStream;
import java.io.IOException;

/**
 * loads the well-known classic James configuration file
 *
  * TODO make this thing be based on Resource class and inject resource.getInputStream() into InputSource 
 */
public class AvalonConfigurationFileProvider implements ConfigurationProvider, ApplicationContextAware {

    private String absoluteFilePath;
    private List configurationInterceptors;
    private ApplicationContext applicationContext;
    private String configuration;

    public void setConfigurationResource(String configuration) {
        this.configuration = configuration;
    }


    public Configuration getConfiguration() {
        Resource resource = applicationContext.getResource(configuration);
        InputStream inputStream;
        try {
            inputStream = resource.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("could not locate configuration file " + configuration, e);
        }
        InputSource inputSource = new InputSource(inputStream);
        Configuration configuration;
        try
        {
            inputSource.setSystemId(resource.getURL().toString());
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

    public void setConfigurationInterceptors(List configurationInterceptors) {
        this.configurationInterceptors = configurationInterceptors;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
