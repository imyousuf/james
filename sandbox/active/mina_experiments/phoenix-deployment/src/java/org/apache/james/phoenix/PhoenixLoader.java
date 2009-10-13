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

public class PhoenixLoader implements LoaderService, ApplicationListener, LogEnabled {

    private Logger logger;
    
    private final Map<String, Object> servicesByName;

    public PhoenixLoader() {
        servicesByName = new HashMap<String, Object>();
        servicesByName.put("org.apache.james.LoaderService", this);
    }
    
    public Object get(String name) {
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
            injectResources(resource);
        }
        
        try {
            for (Object resource : servicesByName.values()) {
                postConstruct(resource);
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
