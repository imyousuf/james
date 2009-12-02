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


package org.apache.james.management.impl;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
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
import org.apache.james.management.BayesianAnalyzerManagementException;
import org.apache.james.management.BayesianAnalyzerManagementMBean;
import org.apache.james.management.BayesianAnalyzerManagementService;
import org.apache.james.services.FileSystem;
import org.apache.james.util.ConfigurationAdapter;
import org.apache.james.bridge.GuiceInjected;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

public class AvalonBayesianAnalyzerManagement implements GuiceInjected, BayesianAnalyzerManagementMBean, BayesianAnalyzerManagementService, Serviceable, Initializable, Configurable, LogEnabled{

    private BayesianAnalyzerManagement mgmt;
    private ConfigurationAdapter config;
    private AvalonLogger logger;
    private DataSourceSelector selector;
    private FileSystem fs;
    
    public void service(ServiceManager manager) throws ServiceException {
        selector = (DataSourceSelector) manager.lookup(DataSourceSelector.ROLE);
        fs = (FileSystem) manager.lookup(FileSystem.ROLE);
    }

    public void initialize() throws Exception {
        mgmt = Guice.createInjector( new BayesianAnalyzerManagementModule(), new AbstractModule() {

            @Override
            protected void configure() {
                bind(org.apache.commons.configuration.HierarchicalConfiguration.class).toInstance(config);
                bind(Log.class).toInstance(logger);
                bind(FileSystem.class).toInstance(fs);     
                bind(DataSourceSelector.class).toInstance(selector);

            }
            
        }).getInstance(BayesianAnalyzerManagement.class);
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

    public void enableLogging(Logger logger) {
        this.logger = new AvalonLogger(logger);        
    }

    public int addHamFromDir(String dir) throws BayesianAnalyzerManagementException {
        return mgmt.addHamFromDir(dir);
    }

    public int addHamFromMbox(String file) throws BayesianAnalyzerManagementException {
        return mgmt.addHamFromMbox(file);
    }

    public int addSpamFromDir(String dir) throws BayesianAnalyzerManagementException {
        return mgmt.addSpamFromDir(dir);
    }

    public int addSpamFromMbox(String file) throws BayesianAnalyzerManagementException {
        return mgmt.addSpamFromMbox(file);
    }

    public void exportData(String file) throws BayesianAnalyzerManagementException {
        mgmt.exportData(file);
    }

    public void importData(String file) throws BayesianAnalyzerManagementException {
        mgmt.importData(file);
    }

    public void resetData() throws BayesianAnalyzerManagementException {
        mgmt.resetData();
    }

}
