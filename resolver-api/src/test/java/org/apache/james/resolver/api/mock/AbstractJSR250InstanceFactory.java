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
package org.apache.james.resolver.api.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.resolver.api.InstanceFactory;
import org.apache.james.resolver.api.InstanceFactory.InstanceException;

/**
 * Abstract base class which implements a JSR250 based LoaderService
 * 
 * 
 */
public abstract class AbstractJSR250InstanceFactory implements InstanceFactory {

    private List<Object> loaderRegistry = new ArrayList<Object>();

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.kernel.Factory#newInstance(java.lang.String)
     */
    public final <T> T newInstance(Class<T> clazz) throws InstanceException {
        return newInstance(clazz, null, null);
    }

    public  final <T> T newInstance(Class<T> clazz, Log log, HierarchicalConfiguration config) throws InstanceException {
        try {
            T obj = clazz.newInstance();
            injectResources(obj);
            postConstruct(obj);
            synchronized (this) {
                loaderRegistry.add(obj);
            }
            return obj;
        } catch (Exception e) {
            throw new InstanceException("Unable to load instance of class " + clazz, e);
        }
    }

    protected Object create(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(className).newInstance();
    }
    /**
     * Dispose all loaded instances by calling the method of the instances which
     * is annotated with @PreDestroy
     */
    public synchronized void dispose() {
        for (int i = 0; i < loaderRegistry.size(); i++) {
            try {
                preDestroy(loaderRegistry.get(i));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        loaderRegistry.clear();
    }

    private void postConstruct(Object resource) throws IllegalAccessException, InvocationTargetException {
        Method[] methods = resource.getClass().getMethods();
        for (Method method : methods) {
            PostConstruct postConstructAnnotation = method.getAnnotation(PostConstruct.class);
            if (postConstructAnnotation != null) {
                Object[] args = {};
                method.invoke(resource, args);

            }
        }
    }

    private void preDestroy(Object resource) throws IllegalAccessException, InvocationTargetException {
        Method[] methods = resource.getClass().getMethods();
        for (Method method : methods) {
            PreDestroy preDestroyAnnotation = method.getAnnotation(PreDestroy.class);
            if (preDestroyAnnotation != null) {
                Object[] args = {};
                method.invoke(resource, args);

            }
        }
    }

    private void injectResources(Object resource) {
        final Method[] methods = resource.getClass().getMethods();
        for (Method method : methods) {
            final Resource resourceAnnotation = method.getAnnotation(Resource.class);
            if (resourceAnnotation != null) {
                final String name = resourceAnnotation.name();
                if (name == null) {
                    throw new UnsupportedOperationException("Resource annotation without name specified is not supported by this implementation");
                } else {
                    // Name indicates a service
                    final Object service = getObjectForName(name);

                    if (service == null) {
                        throw new RuntimeException("Injection failed for object " + resource + " on method " + method + " with resource name " + name + ", because no mapping was found");
                    } else {
                        try {
                            Object[] args = { service };
                            method.invoke(resource, args);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Injection failed for object " + resource + " on method " + method + " with resource " + service, e);
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException("Injection failed for object " + resource + " on method " + method + " with resource " + service, e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException("Injection failed for object " + resource + " on method " + method + " with resource " + service, e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Return the Object which should be injected for given name
     * 
     * @param name
     * @return object
     */
    public abstract Object getObjectForName(String name);
}
