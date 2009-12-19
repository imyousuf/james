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

package org.apache.james.api.kernel.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.api.kernel.LoaderService;

public class FakeLoader implements LoaderService, org.apache.avalon.framework.service.ServiceManager{

    private final Map<String, Object> servicesByName;
    private final Map<String, String> mappings = new HashMap<String, String>();
    public FakeLoader() {
        servicesByName = new HashMap<String, Object>();
        servicesByName.put("org.apache.james.LoaderService", this);
        
        mappings.put("James", "org.apache.james.services.MailServer");
        mappings.put("filesystem", "org.apache.james.services.FileSystem");
        mappings.put("dnsserver", "org.apache.james.api.dnsservice.DNSService");
        mappings.put("mailstore", "org.apache.avalon.cornerstone.services.store.Store");
        mappings.put("users-store", "org.apache.james.api.user.UsersStore");
        mappings.put("localusersrepository", "org.apache.james.api.user.UsersRepository");
        mappings.put("spoolrepository", "org.apache.james.services.SpoolRepository");
        mappings.put("domainlist", "org.apache.james.api.domainlist.DomainList");
        mappings.put("sockets", "org.apache.avalon.cornerstone.services.sockets.SocketManager");
        mappings.put("scheduler", "org.apache.avalon.cornerstone.services.scheduler.TimeScheduler");
        mappings.put("database-connections", "org.apache.avalon.cornerstone.services.datasources.DataSourceSelector");
        mappings.put("defaultvirtualusertable", "org.apache.james.api.vut.VirtualUserTable");
   
        mappings.put("spoolmanager", "org.apache.james.services.SpoolManager");
        mappings.put("matcherpackages", "org.apache.james.transport.MatcherLoader");
        mappings.put("mailetpackages", "org.apache.james.transport.MailetLoader");
        mappings.put("virtualusertable-store", "org.apache.james.api.vut.VirtualUserTableStore");
        mappings.put("imapserver", "org.org.apache.jsieve.mailet.Poster");
        mappings.put("threadmanager", "org.apache.avalon.cornerstone.services.threads.ThreadManager");
        mappings.put("spoolmanagement", "org.apache.james.management.SpoolManagementService");
        mappings.put("bayesiananalyzermanagement", "org.apache.james.management.BayesianAnalyzerManagementService");
        mappings.put("processormanagement", "org.apache.james.management.ProcessorManagementService");
        mappings.put("virtualusertablemanagement", "org.apache.james.api.vut.management.VirtualUserTableManagementService");
        mappings.put("domainlistmanagement", "org.apache.james.management.DomainListManagementService");
        mappings.put("nntp-repository", "org.apache.james.nntpserver.repository.NNTPRepository");
    }
    
    
    public Object get(String name) { 
        Object service = servicesByName.get(mapName(name));
        
        System.out.println("KEYS="+servicesByName.keySet().toString());
        return service;
    }
    
    private String mapName(String name) {
        String newName = mappings.get(name);
        if(newName == null) {
            newName = name;
        }
        System.out.println("NEW=" + newName);
        return newName;
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
                    
                    System.out.println("SERVICE=" + service);
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
