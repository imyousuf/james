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
package org.apache.james.container.spring;

import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public class SpringVirtualUserTableStore extends AbstractStore implements VirtualUserTableStore{

    private String defaultName;


    /*
     * (non-Javadoc)
     * @see org.apache.james.container.spring.AbstractStore#getSubConfigurations(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected List<HierarchicalConfiguration> getSubConfigurations(HierarchicalConfiguration rootConf) {
        return rootConf.configurationsAt("table");
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.vut.VirtualUserTableStore#getTable(java.lang.String)
     */
    public VirtualUserTable getTable(String name) {
        if (name == null || name.trim().equals("")) {
            name = defaultName;
        }

        VirtualUserTable response = null;

        if (objects.contains(name)) {
            try {
                response = (VirtualUserTable) context.getBean(name, VirtualUserTable.class);
            } catch (NoSuchBeanDefinitionException e) {
                // Just catch the exception
            }
        }
        if ((response == null) && (log.isWarnEnabled())) {
            log.warn("No VirtualUserTable called: " + name);
        }
        return response;   
    }


    /**
     * Set the default VirtualUserTable which will get returned when no name is given or the name is empty
     * 
     * @param defaultName
     */
    public void setDefaultTable(String defaultName) {
        this.defaultName = defaultName;
    }
    
}
