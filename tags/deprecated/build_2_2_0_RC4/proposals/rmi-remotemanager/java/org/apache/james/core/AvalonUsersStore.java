/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.avalon.phoenix.Block;

/**
 *
 */
public class AvalonUsersStore
    extends AbstractLogEnabled
    implements Block, Contextualizable, Composable, Configurable, Initializable, UsersStore {

    private HashMap repositories;
    protected Context                context;
    protected Configuration          configuration;
    protected ComponentManager       componentManager;


    public void contextualize(final Context context)
            throws ContextException {
        this.context = context;
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException {
        this.configuration = configuration;
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        this.componentManager = componentManager;
    }

    public void initialize()
        throws Exception {

        getLogger().info("AvalonUsersStore init...");
        repositories = new HashMap();

        Configuration[] repConfs = configuration.getChildren("repository");
        for ( int i = 0; i < repConfs.length; i++ )
        {
            Configuration repConf = repConfs[i];
            String repName = repConf.getAttribute("name");
            String repClass = repConf.getAttribute("class");

	    getLogger().debug("Starting " + repClass);
            UsersRepository rep = (UsersRepository) Class.forName(repClass).newInstance();

            setupLogger((Component)rep);

            if (rep instanceof Contextualizable) {
                ((Contextualizable) rep).contextualize(context);
            }
            if (rep instanceof Composable) {
                ((Composable) rep).compose( componentManager );
            }
            if (rep instanceof Configurable) {
                ((Configurable) rep).configure(repConf);
            }
            if (rep instanceof Initializable) {
                ((Initializable) rep).initialize();
            }
            repositories.put(repName, rep);
            getLogger().info("UsersRepository " + repName + " started.");
        }
        getLogger().info("AvalonUsersStore ...init");
    }


    public UsersRepository getRepository(String request) {
        UsersRepository response = (UsersRepository) repositories.get(request);
        if (response == null) {
            getLogger().warn("No users repository called: " + request);
        }
        return response;
    }

    public Iterator getRepositoryNames() {
        return this.repositories.keySet().iterator();
    }
    
}
