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
package org.apache.james.nntpserver.repository;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.bridge.GuiceInjected;
import org.apache.james.services.FileSystem;
import org.apache.james.util.ConfigurationAdapter;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonNNTPRepository implements NNTPRepository, GuiceInjected, Configurable, Serviceable, LogEnabled, Initializable{

    private NNTPRepository repos;
    private FileSystem fs;
    private AvalonLogger logger;
    private ConfigurationAdapter config;
    
    public void createArticle(InputStream in) {
        repos.createArticle(in);
    }

    public NNTPArticle getArticleFromID(String id) {
        return repos.getArticleFromID(id);
    }

    public Iterator<NNTPArticle> getArticlesSince(Date dt) {
        return repos.getArticlesSince(dt);
    }

    public NNTPGroup getGroup(String groupName) {
        return repos.getGroup(groupName);
    }

    public Iterator<NNTPGroup> getGroupsSince(Date dt) {
        return repos.getGroupsSince(dt);
    }

    public Iterator<NNTPGroup> getMatchedGroups(String wildmat) {
        return repos.getMatchedGroups(wildmat);
    }

    public String[] getOverviewFormat() {
        return repos.getOverviewFormat();
    }

    public boolean isReadOnly() {
        return repos.isReadOnly();
    }

    public void service(ServiceManager arg0) throws ServiceException {
        fs = (FileSystem) arg0.lookup(FileSystem.ROLE);
    }

    public void enableLogging(Logger arg0) {
        this.logger = new AvalonLogger(arg0);
    }

    public void initialize() throws Exception {
        repos = Guice.createInjector(new Jsr250Module(), new AbstractModule() {
            
            @Override
            protected void configure() {
                bind(org.apache.commons.configuration.HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
                bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
                bind(FileSystem.class).annotatedWith(Names.named("org.apache.james.services.FileSystem")).toInstance(fs);
            }
        }).getInstance(NNTPRepositoryImpl.class);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException {
        try {
            this.config = new ConfigurationAdapter(config);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Unable to convert configuration", e);
        }
    }

}
