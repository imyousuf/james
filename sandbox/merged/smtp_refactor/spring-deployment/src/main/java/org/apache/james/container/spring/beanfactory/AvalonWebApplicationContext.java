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
package org.apache.james.container.spring.beanfactory;

import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.BeansException;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * application context which can be initialized in a web container environment
 */
public class AvalonWebApplicationContext extends AbstractRefreshableWebApplicationContext {

    private List jamesAssemblyResources = new ArrayList();
    private List springBeanLocations = new ArrayList();
    
    protected void loadBeanDefinitions(DefaultListableBeanFactory defaultListableBeanFactory) throws IOException, BeansException {
        // for the sake of simplicity, support only one james assembly and one bean definition currently
        if (jamesAssemblyResources.size() != 1) throw new RuntimeException("can only load one Avalon-type assembly file");
        if (springBeanLocations.size() != 1) throw new RuntimeException("can only load one Spring bean definition file");
        Resource springBeanResource = getResourceByPath((String) springBeanLocations.get(0));
        Resource jamesAssemblyResource = (Resource) jamesAssemblyResources.get(0);
        AvalonApplicationContext.loadAvalonBasedBeanDefinitions(defaultListableBeanFactory, springBeanResource, jamesAssemblyResource);
    }

    public void setConfigLocations(String[] locationStrings) {
        if (locationStrings == null) locationStrings = new String[] {"/WEB-INF/james-assembly.xml", "/WEB-INF/spring-beans.xml"};
        for (int i = 0; i < locationStrings.length; i++) {
            String locationString = locationStrings[i];
            if (locationString.indexOf("james-assembly") != -1) jamesAssemblyResources.add(getResourceByPath(locationString));
            else springBeanLocations.add(locationString);
        }
    }
}
