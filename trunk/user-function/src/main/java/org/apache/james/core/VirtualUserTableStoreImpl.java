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



package org.apache.james.core;

import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.VirtualUserTableStore;

/**
 * Provides a registry of VirtualUserTables
 *
 */
public class VirtualUserTableStoreImpl
    extends AbstractStore<VirtualUserTable>
    implements VirtualUserTableStore {


    private String defaultName;


    /** 
     * Get the repository, if any, whose name corresponds to
     * the argument parameter
     *
     * @param name the name of the desired repository
     *
     * @return the VirtualUserTable corresponding to the name parameter
     */
    public VirtualUserTable getTable(String name) {
        if (name == null || name.trim().equals("")) {
            name = defaultName;
        }
        VirtualUserTable response = getObject(name);
        if ((response == null) && (getLogger().isWarnEnabled())) {
            getLogger().warn("No virtualUserTable called: " + name);
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
    
    /**
     * @see org.apache.james.core.AbstractAvalonStore#getStoreName()
     */
    public String getStoreName() {
        return "VirtualUserTableStoreImpl";
    }


    /**
     * @see org.apache.james.core.AbstractStore#getConfigurations(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    @SuppressWarnings("unchecked")
    public List<HierarchicalConfiguration> getConfigurations(
            HierarchicalConfiguration config) {
        return config.configurationsAt("table");
    }
}
