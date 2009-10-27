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
package org.apache.james.domain;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.services.FileSystem;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonJDBCDomainList extends AbstractAvalonDomainList{

    private DataSourceSelector datasources;
    private FileSystem fs;

    @Override
    public void service(ServiceManager arg0) throws ServiceException {
        super.service(arg0);
        datasources = (DataSourceSelector)arg0.lookup(DataSourceSelector.ROLE); 
        fs = (FileSystem) arg0.lookup(FileSystem.ROLE);
    }

    public void initialize() throws Exception {
        domList = Guice.createInjector(new Jsr250Module(), new AbstractAvalonDomainModule() {

            @Override
            protected void configure() {
                super.configure();
                bind(FileSystem.class).annotatedWith(Names.named("org.apache.james.services.FileSystem")).toInstance(fs);
                bind(DataSourceSelector.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.datasources.DataSourceSelector")).toInstance(datasources);
            }
            
        }).getInstance(JDBCDomainList.class);
    }

}
