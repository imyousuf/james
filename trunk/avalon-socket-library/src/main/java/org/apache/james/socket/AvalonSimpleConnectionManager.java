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
package org.apache.james.socket;

import java.net.ServerSocket;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
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
import org.apache.excalibur.thread.ThreadPool;
import org.apache.james.bridge.GuiceInjected;
import org.apache.james.util.ConfigurationAdapter;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonSimpleConnectionManager  implements LogEnabled, Initializable, GuiceInjected, Serviceable, Configurable, JamesConnectionManager {

    private AvalonLogger logger;
    private ConfigurationAdapter config;
    private ThreadManager threadManager;
    private JamesConnectionManager manager;
    
    /**
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) {
        this.logger = new AvalonLogger(logger);
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


    public void service(ServiceManager manager) throws ServiceException {
        threadManager = (ThreadManager) manager.lookup(ThreadManager.ROLE);
    }

    public void connect(String name, ServerSocket socket, ConnectionHandlerFactory handlerFactory, ThreadPool threadPool, int maxOpenConnections) throws Exception {
        manager.connect(name, socket, handlerFactory, threadPool, maxOpenConnections);
        
    }

    public void connect(String name, ServerSocket socket, ConnectionHandlerFactory handlerFactory, int maxOpenConnections) throws Exception {
        manager.connect(name, socket, handlerFactory, maxOpenConnections);
        
    }

    public void connect(String name, ServerSocket socket, ConnectionHandlerFactory handlerFactory, ThreadPool threadPool) throws Exception {
        manager.connect(name, socket, handlerFactory, threadPool);
        
    }

    public void connect(String name, ServerSocket socket, ConnectionHandlerFactory handlerFactory) throws Exception {
        manager.connect(name, socket, handlerFactory);
        
    }

    public void connect(String name, ServerSocket socket, ConnectionHandlerFactory handlerFactory, ThreadPool threadPool, int maxOpenConnections, int maxOpenConnectionsPerIP) throws Exception {
        manager.connect(name, socket, handlerFactory, threadPool, maxOpenConnections, maxOpenConnectionsPerIP);
        
    }

    public void connect(String name, ServerSocket socket, ConnectionHandlerFactory handlerFactory, int maxOpenConnections, int maxOpenConnectionsPerIP) throws Exception {
        manager.connect(name, socket, handlerFactory, maxOpenConnections, maxOpenConnectionsPerIP);
    }

    public int getMaximumNumberOfOpenConnections() {
        return manager.getMaximumNumberOfOpenConnections();
    }

    public int getMaximumNumberOfOpenConnectionsPerIP() {
        return manager.getMaximumNumberOfOpenConnectionsPerIP();
    }

    public void disconnect(String arg0) throws Exception {
        manager.disconnect(arg0);
    }

    public void disconnect(String arg0, boolean arg1) throws Exception {
        manager.disconnect(arg0, arg1);
    }


    public void initialize() throws Exception {
        manager = Guice.createInjector(new Jsr250Module() , new AbstractModule() {

            @Override
            protected void configure() {
                bind(ThreadManager.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.threads.ThreadManager")).toInstance(threadManager);
                bind(org.apache.commons.configuration.HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
                bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
            }
            
        }).getInstance(SimpleConnectionManager.class);
    }

}
