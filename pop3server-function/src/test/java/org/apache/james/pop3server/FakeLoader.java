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

package org.apache.james.pop3server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.api.kernel.LoaderService;

public class FakeLoader implements LoaderService, org.apache.avalon.framework.service.ServiceManager{

    private final Map<String, Object> servicesByName;
    
    public FakeLoader() {
        servicesByName = new HashMap<String, Object>();
        servicesByName.put("org.apache.james.LoaderService", this);
    }
    
    
    public Object get(String name) {
        Object service = servicesByName.get(name);
        return service;
    }
    
    private void injectResources(Object resource) {
        final Method[] methods = resource.getClass().getMethods();
        for (Method method : methods) {
            final Resource resourceAnnotation = method.getAnnotation(Resource.class);
            if (resourceAnnotation != null) {
                final String name = resourceAnnotation.name();
                if (name == null) {
                    // Unsupported
                } else {
                    // Name indicates a service
                    final Object service = get(name);
                    if (service == null) {
                   } else {
                        try {
                            Object[] args = {service};
                            method.invoke(resource, args);
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
    
    public <T> T load(Class<T> type) {
        try {
            final T newInstance = type.newInstance();
            injectResources(newInstance);
            return newInstance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public boolean hasService(String name) {
        return servicesByName.containsKey(name);
    }

    public Object lookup(String name) throws ServiceException {
        return servicesByName.get(name);
    }

    public void release(Object service) {
    }

    public void put(String role, Object service) {
        servicesByName.put(role, service);
    }
}
