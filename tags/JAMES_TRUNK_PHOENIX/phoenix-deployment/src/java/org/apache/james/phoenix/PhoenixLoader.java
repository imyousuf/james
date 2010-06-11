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

package org.apache.james.phoenix;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.phoenix.ApplicationEvent;
import org.apache.avalon.phoenix.ApplicationListener;
import org.apache.avalon.phoenix.BlockEvent;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.bridge.GuiceInjected;

public class PhoenixLoader implements LoaderService, ApplicationListener, LogEnabled {

    private Logger logger;
    
    private final Map<String, Object> servicesByName;
    private final Map<String, String> nameMappings;
    public PhoenixLoader() {
        nameMappings = new HashMap<String, String>();
        setupNameMappings();
        servicesByName = new HashMap<String, Object>();     
        servicesByName.put("org.apache.james.LoaderService", this);
    }
    
    /**
     * This is really a ugly thing put it let us refactor step by step
     */
    private void setupNameMappings() {
        nameMappings.put("org.apache.james.services.MailServer", "James");
        nameMappings.put("org.apache.mailet.MailetContext", "James");
        nameMappings.put("org.apache.james.services.FileSystem", "filesystem");
        nameMappings.put("org.apache.james.api.dnsservice.DNSService", "dnsserver");
        nameMappings.put("org.apache.james.services.MailServer", "James");
        nameMappings.put("org.apache.james.api.user.UsersRepository", "localusersrepository");
        nameMappings.put("org.apache.james.services.SpoolRepository", "spoolrepository");
        nameMappings.put("org.apache.james.api.domainlist.DomainList", "domainlist");
        nameMappings.put("org.apache.avalon.cornerstone.services.sockets.SocketManager", "sockets");
        nameMappings.put("org.apache.james.services.SpoolRepository", "spoolrepository");
        nameMappings.put("org.apache.avalon.cornerstone.services.scheduler.TimeScheduler", "scheduler");
        nameMappings.put("org.apache.avalon.cornerstone.services.datasources.DataSourceSelector", "database-connections");
        nameMappings.put("org.apache.james.api.vut.VirtualUserTable", "defaultvirtualusertable");
        nameMappings.put("org.apache.james.transport.MatcherLoader", "matcherpackages");
        nameMappings.put("org.apache.james.transport.MailetLoader", "mailetpackages");
        nameMappings.put("org.apache.avalon.cornerstone.services.store.Store", "mailstore");
        nameMappings.put("org.apache.james.transport.MatcherLoader", "matcherpackages");
        nameMappings.put("org.apache.james.api.user.UsersStore", "users-store");


        nameMappings.put("org.apache.james.socket.JamesConnectionManager", "connections");
        nameMappings.put("org.apache.avalon.cornerstone.services.threads.ThreadManager", "thread-manager");
        nameMappings.put("org.apache.james.management.SpoolManagementService", "spoolmanagement");
        nameMappings.put("org.apache.james.management.BayesianAnalyzerManagementService", "bayesiananalyzermanagement");
        nameMappings.put("org.apache.james.management.ProcessorManagementService", "processormanagement");

        nameMappings.put("org.apache.james.api.vut.management.VirtualUserTableManagementService", "virtualusertablemanagement");
        nameMappings.put("org.apache.james.management.DomainListManagementService", "domainlistmanagement");
    }
    
    public Object get(String name) {
        // re-map if needed
        if (nameMappings.containsKey(name)) {
            name = nameMappings.get(name);
        }
        Object service = servicesByName.get(name);
        return service;
    }

    public void applicationFailure(Exception exception) {
        
    }

    /**
     * Indicates application has started.
     * This hook initialises all annotated resources.
     * This ensure that all services have been loaded 
     * before initilisation any.
     */
    public void applicationStarted() {
        for (Object resource : servicesByName.values()) {
            
            // Only handle injection if it not use guice already 
            if ((resource instanceof GuiceInjected) == false) {
                injectResources(resource);
            }
        }
        
        try {
            for (Object resource : servicesByName.values()) {
                // Only handle injection if it not use guice already 
                if ((resource instanceof GuiceInjected) == false) {
                    postConstruct(resource);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Initialisation failed", e);
        }
    }

    private void postConstruct(Object resource) throws IllegalAccessException,
            InvocationTargetException {
        Method[] methods = resource.getClass().getMethods();
        for (Method method : methods) {
            PostConstruct postConstructAnnotation = method.getAnnotation(PostConstruct.class);
            if (postConstructAnnotation != null) {
                Object[] args = {};
                method.invoke(resource, args);
                if (logger.isDebugEnabled()) {
                    logger.debug("Calling PostConstruct on " + resource);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void injectResources(Object resource) {
        final Method[] methods = resource.getClass().getMethods();
        for (Method method : methods) {
            final Resource resourceAnnotation = method.getAnnotation(Resource.class);
            if (resourceAnnotation != null) {
                final String name = resourceAnnotation.name();
                if (name == null) {
                    @SuppressWarnings("unused")
                    final Class type = resourceAnnotation.type();
                    // TODO: use Guice 
                } else {
                    // Name indicates a service
                    final Object service = get(name);
                    if (service == null) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Unknown service: "  + name);
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug(servicesByName.toString());
                        }
                   } else {
                        try {
                            Object[] args = {service};
                            method.invoke(resource, args);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Set service " + service + " on " + resource);
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Injection failed", e);
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException("Injection failed", e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException("Injection failed", e);
                        }
                    }
                }
            }
        }
    }

    public void applicationStarting(ApplicationEvent event) throws Exception {
        
    }

    public void applicationStopped() {
        
    }

    public void applicationStopping() {
        
    }

    /**
     * Adds service.
     */
    public void blockAdded(BlockEvent event) {
        final Object resource = event.getObject();
        final String name = event.getName();
        servicesByName.put(name, resource);
        // TODO: Add to dynamic Guice module 
        // TODO: This should allow access to Pheonix loaded services
    }

    /**
     * Removes service.
     * Existing references are maintained.
     */
    public void blockRemoved(BlockEvent event) {
        final String name = event.getName();
        servicesByName.remove(name);
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    public <T> T load(Class<T> type) {
        try {
            // TODO: Use Guice to load type
            final T base = type.newInstance();
            injectResources(base);
            return base;
        } catch (InstantiationException e) {
            logger.warn("Cannot instantiate type", e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.warn("Cannot instantiate type", e);
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            logger.warn("Cannot instantiate type", e);
            throw new RuntimeException(e);
        }
    }
}
