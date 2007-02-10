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

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Provides a registry of objects
 *
 */
public abstract class AbstractAvalonStore
    extends AbstractLogEnabled
    implements Serviceable, Configurable, Initializable {

    private HashMap objects;

    /**
     * The Avalon configuration used by the instance
     */
    protected Configuration          configuration;

    /**
     * The Avalon component manager used by the instance
     */
    protected ServiceManager       manager;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service( final ServiceManager manager )
        throws ServiceException {
        this.manager = manager;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException {
        this.configuration = configuration;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize()
        throws Exception {

        getLogger().info(getStoreName() + " init...");
        objects = new HashMap();

        Configuration[] repConfs = getConfigurations(configuration);
        ClassLoader theClassLoader = null;
        for ( int i = 0; i < repConfs.length; i++ )
        {
            Configuration repConf = repConfs[i];
            String repName = repConf.getAttribute("name");
            String repClass = repConf.getAttribute("class");

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Starting " + repClass);
            }

            if (theClassLoader == null) {
                theClassLoader = Thread.currentThread().getContextClassLoader();
            }

            Object object = getClassInstance(theClassLoader,repClass);

            setupLogger(object);

            ContainerUtil.service(object,manager);

            ContainerUtil.configure(object,repConf);
            ContainerUtil.initialize(object);
            
            
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
    protected Object getObject(String name) {
        return objects.get(name);
    }

    /** 
     * Yield an <code>Iterator</code> over the set of object
     * names managed internally by this store.
     *
     * @return an Iterator over the set of repository names
     *         for this store
     */
    protected Iterator getObjectNames() {
        return this.objects.keySet().iterator();
    }
    
    /**
     * Return new Object of the loader classname
     * 
     * @param loader the ClassLoader
     * @param className the classname
     * @return the loaded Objected
     * @throws Exception
     */
    public abstract Object getClassInstance(ClassLoader loader, String className) throws Exception;
    
    /**
     * Return the Store configurations 
     * 
     * @param config the main config
     * @return configurations
     */
    public abstract Configuration[] getConfigurations(Configuration config);
    
    /**
     * Return the Store name which should be used for logging
     * 
     * @return the name
     */
    public abstract String getStoreName();
    
}
