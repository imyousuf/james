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
package org.apache.james.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.name.Names;

/**
 * Abstract base class for Stores which use Guice to load stuff
 *
 * @param <Type>
 */
public abstract class AbstractGuiceStore<Type> {

    private HashMap<String,Type> objects;
    
    protected Log logger;

    protected HierarchicalConfiguration config;
    
    @Resource(name="org.apache.commons.logging.Log")
    public void setLogger(Log logger) {
        this.logger = logger;
    }
    
    protected Log getLogger() {
        return logger;
    }
    
    @Resource(name="org.apache.commons.configuration.Configuration")
    public void setConfiguration(HierarchicalConfiguration config) {
        this.config = config;
    }   

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    @SuppressWarnings("unchecked")
    @PostConstruct
    public void init()
        throws Exception {

        getLogger().info(getStoreName() + " init...");
        objects = new HashMap<String,Type>();

        List<HierarchicalConfiguration> repConfs = getConfigurations(config);
        ClassLoader theClassLoader = null;
        for ( int i = 0; i < repConfs.size(); i++ )
        {
            final HierarchicalConfiguration repConf = repConfs.get(i);
            String repName = repConf.getString("[@name]");
            String repClass = repConf.getString("[@class]");

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Starting " + repClass);
            }
            if (theClassLoader == null) {
                theClassLoader = Thread.currentThread().getContextClassLoader();
            }            
            Type object = (Type) Guice.createInjector(getModule(),new Jsr250Module(), new AbstractModule() {

                @Override
                protected void configure() {
                    bind(HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(repConf);
                }
                
            }).getInstance(theClassLoader.loadClass(repClass));
            
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Load instance " + object);
            }
            
            objects.put(repName, object);
            if (getLogger().isInfoEnabled()) {
                StringBuffer logBuffer = 
                    new StringBuffer(64)
                            .append("Store  ")
                            .append(repName)
                            .append(" started.");
                getLogger().info(logBuffer.toString());
            }
        }
    }


    /** 
     * Get the object, if any, whose name corresponds to
     * the argument parameter
     *
     * @param name the name of the desired object
     *
     * @return the Object corresponding to the name parameter
     */
    protected Type getObject(String name) {
        return objects.get(name);
    }

    /** 
     * Yield an <code>Iterator</code> over the set of object
     * names managed internally by this store.
     *
     * @return an Iterator over the set of repository names
     *         for this store
     */
    protected Iterator<String> getObjectNames() {
        return this.objects.keySet().iterator();
    }
    
    
    /**
     * Return the Store configurations 
     * 
     * @param config the main config
     * @return configurations
     */
    public abstract List<HierarchicalConfiguration> getConfigurations(HierarchicalConfiguration config);
    
    /**
     * Return the Store name which should be used for logging
     * 
     * @return the name
     */
    public abstract String getStoreName();
    
    /**
     * Return the Module to use for Injecting
     * 
     * @return module
     */
    protected abstract Module getModule();
    
}
