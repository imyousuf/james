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
package org.apache.james.api.kernel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;

/**
 * Abstract base class which implements a JSR250 based LoaderService
 * 
 *
 */
public abstract class AbstractJSR250LoaderService implements LoaderService{

	 /*
	 * (non-Javadoc)
	 * @see org.apache.james.api.kernel.LoaderService#injectDependencies(java.lang.Object)
	 */
	public void injectDependencies(Object obj) {
        try {
            injectResources(obj);
            postConstruct(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to handle dependency injection of object " + obj, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to handle dependency injection of object " + obj, e);
        }
    }

	   
    private void postConstruct(Object resource) throws IllegalAccessException,
            InvocationTargetException {
        Method[] methods = resource.getClass().getMethods();
        for (Method method : methods) {
            PostConstruct postConstructAnnotation = method
                    .getAnnotation(PostConstruct.class);
            if (postConstructAnnotation != null) {
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
                            Object[] args = {service};
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

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.api.kernel.LoaderService#injectDependenciesWithLifecycle(java.lang.Object, org.apache.commons.logging.Log, org.apache.commons.configuration.HierarchicalConfiguration)
	 */
	public void injectDependenciesWithLifecycle(Object obj, Log logger,
			HierarchicalConfiguration config) {
		if (obj instanceof LogEnabled) {
			((LogEnabled) obj).setLog(logger);
		}
		if (obj instanceof Configurable) {
			try {
			((Configurable) obj).configure(config);
			} catch (ConfigurationException ex) {
				throw new RuntimeException("Unable to configure object " + obj, ex);
			}
		}
		injectDependencies(obj);
	}
	
	/**
	 * Return the Object which should be injected for given name
	 * 
	 * @param name
	 * @return object
	 */
	protected abstract Object getObjectForName(String name);
}
