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
package org.apache.james.vut;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.impl.vut.AbstractAvalonVirtualUserTable;
import org.apache.james.impl.vut.AbstractVirtualUserTable;
import org.apache.james.services.FileSystem;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonJDBCVirtualUserTable extends AbstractAvalonVirtualUserTable{

    private FileSystem fs;
    private DataSourceSelector selector;
    private JDBCVirtualUserTable table;
  
    @Override
    protected AbstractVirtualUserTable getVirtualUserTable() {
        return table;
    }

    public void initialize() throws Exception {
        table = Guice.createInjector(new Jsr250Module(), new AvalonJDBCVirtualUserTableModule()).getInstance(JDBCVirtualUserTable.class);
    }
    
    
    @Override
    public void service(ServiceManager manager) throws ServiceException {
        super.service(manager);
        fs = (FileSystem) manager.lookup(FileSystem.ROLE);
        selector = (DataSourceSelector) manager.lookup(DataSourceSelector.ROLE);
    }


    private class AvalonJDBCVirtualUserTableModule extends BaseAvalonVirtualUserTableModule {

        @Override
        protected void configure() {
            super.configure();
            bind(FileSystem.class).annotatedWith(Names.named("org.apache.james.services.FileSystem")).toInstance(fs);
            bind(DataSourceSelector.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.datasources.DataSourceSelector")).toInstance(selector);
        }
        
    }

}
