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

import org.apache.avalon.framework.configuration.Configuration;

import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.VirtualUserTableStore;

import java.util.Iterator;

/**
 * Provides a registry of VirtualUserTables
 *
 */
public class AvalonVirtualUserTableStore
    extends AbstractAvalonStore
    implements VirtualUserTableStore {

   
    /** 
     * Get the repository, if any, whose name corresponds to
     * the argument parameter
     *
     * @param name the name of the desired repository
     *
     * @return the VirtualUserTable corresponding to the name parameter
     */
    public VirtualUserTable getTable(String name) {
        VirtualUserTable response = (VirtualUserTable) getObject(name);
        if ((response == null) && (getLogger().isWarnEnabled())) {
            getLogger().warn("No virtualUserTable called: " + name);
        }
        return response;
    }

    /** 
     * Yield an <code>Iterator</code> over the set of repository
     * names managed internally by this store.
     *
     * @return an Iterator over the set of repository names
     *         for this store
     */
    public Iterator getTableNames() {
        return getObjectNames();
    }

    /**
     * @see org.apache.james.core.AbstractAvalonStore#getClassInstance(ClassLoader, String)
     */
    public Object getClassInstance(ClassLoader loader, String repClass) throws Exception {
        return (VirtualUserTable) loader.loadClass(repClass).newInstance();
    }

    /**
     * @see org.apache.james.core.AbstractAvalonStore#getConfigurations(org.apache.avalon.framework.configuration.Configuration)
     */
    public Configuration[] getConfigurations(Configuration config) {
        return configuration.getChildren("table");
    }

    /**
     * @see org.apache.james.core.AbstractAvalonStore#getStoreName()
     */
    public String getStoreName() {
        return "AvalonVirtualUserTableStore";
    }
}
