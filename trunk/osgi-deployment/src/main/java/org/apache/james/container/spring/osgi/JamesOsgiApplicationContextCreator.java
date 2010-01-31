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
package org.apache.james.container.spring.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.DelegatedExecutionOsgiBundleApplicationContext;
import org.springframework.osgi.extender.OsgiApplicationContextCreator;
import org.springframework.osgi.extender.support.ApplicationContextConfiguration;
import org.springframework.osgi.extender.support.scanning.ConfigurationScanner;
import org.springframework.osgi.extender.support.scanning.DefaultConfigurationScanner;

public class JamesOsgiApplicationContextCreator implements OsgiApplicationContextCreator{
    private ConfigurationScanner configurationScanner = new DefaultConfigurationScanner();
    
    /*
     * (non-Javadoc)
     * @see org.springframework.osgi.extender.OsgiApplicationContextCreator#createApplicationContext(org.osgi.framework.BundleContext)
     */
    public DelegatedExecutionOsgiBundleApplicationContext createApplicationContext(BundleContext context) throws Exception {
        Bundle bundle = context.getBundle();
        ApplicationContextConfiguration config = new ApplicationContextConfiguration(bundle, configurationScanner);
        
        if (!config.isSpringPoweredBundle()) {
            return null;
        }

        DelegatedExecutionOsgiBundleApplicationContext sdoac = new JamesOsgiBundleXmlApplicationContext(config.getConfigurationLocations());
        sdoac.setBundleContext(context);
        sdoac.setPublishContextAsService(config.isPublishContextAsService());

        return sdoac;

    }

}
