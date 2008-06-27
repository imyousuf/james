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
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Provides a registry of user repositories.
 *
 */
public class AvalonUsersStore
    extends AbstractLogEnabled
    implements Contextualizable, Serviceable, Configurable, Initializable, UsersStore {

    /**
     * A mapping of respository identifiers to actual repositories
     * This mapping is obtained from the component configuration
     */
    private HashMap repositories;

    /**
     * The Avalon context used by the instance
     */
    protected Context                context;

    /**
     * The Avalon configuration used by the instance
     */
    protected Configuration          configuration;

    /**
     * The Avalon component manager used by the instance
     */
    protected ServiceManager       manager;

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(final Context context)
            throws ContextException {
        this.context = context;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#compose(ServiceManager)
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

        getLogger().info("AvalonUsersStore init...");
        repositories = new HashMap();

        Configuration[] repConfs = configuration.getChildren("repository");
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
                theClassLoader = this.getClass().getClassLoader();
            }

            UsersRepository rep = (UsersRepository) theClassLoader.loadClass(repClass).newInstance();

            setupLogger(rep);

            ContainerUtil.contextualize(rep,context);
            ContainerUtil.service(rep,manager);
            if (rep instanceof Composable) {
                final String error = "no implementation in place to support Composable";
                getLogger().error( error );
                throw new IllegalArgumentException( error );
            }
            ContainerUtil.configure(rep,repConf);
            ContainerUtil.initialize(rep);
            
            repositories.put(repName, rep);
            if (getLogger().isInfoEnabled()) {
                StringBuffer logBuffer = 
                    new StringBuffer(64)
                            .append("UsersRepository ")
                            .append(repName)
                            .append(" started.");
                getLogger().info(logBuffer.toString());
            }
        }
        getLogger().info("AvalonUsersStore ...init");
    }


    /** 
     * Get the repository, if any, whose name corresponds to
     * the argument parameter
     *
     * @param name the name of the desired repository
     *
     * @return the UsersRepository corresponding to the name parameter
     */
    public UsersRepository getRepository(String name) {
        UsersRepository response = (UsersRepository) repositories.get(name);
        if ((response == null) && (getLogger().isWarnEnabled())) {
            getLogger().warn("No users repository called: " + name);
        }
        return response;
    }

    /** 
     * Yield an <code>Iterator</code> over the set of repository
     * names managed internally by this store.
     *
     * @return an Iterator over the set of repository names
     *         for this store
     */
    public Iterator getRepositoryNames() {
        return this.repositories.keySet().iterator();
    }
}
