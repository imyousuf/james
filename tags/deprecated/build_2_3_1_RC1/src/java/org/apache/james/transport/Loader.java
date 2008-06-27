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
package org.apache.james.transport;
import java.io.File;
import java.util.Vector;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.mailet.MailetContext;

/**
 *
 * $Id$
 */
public abstract class Loader extends AbstractLogEnabled implements Contextualizable, Serviceable, Configurable, Initializable {

    protected String baseDirectory = null;
    protected final String MAILET_PACKAGE = "mailetpackage";
    protected final String MATCHER_PACKAGE = "matcherpackage";
    /**
     * The list of packages that may contain Mailets or matchers
     */
    protected Vector packages;

    /**
     * System service manager
     */
    private ServiceManager serviceManager;

    /**
     * Mailet context
     */
    protected MailetContext mailetContext;

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(final Context context) throws ContextException 
    {
        try 
        {
            baseDirectory = ((File)context.get( "app.home") ).getCanonicalPath();
        } 
        catch (Throwable e) 
        {
            getLogger().error( "can't get base directory for mailet loader" );
            throw new ContextException("can't contextualise mailet loader " + e.getMessage(), e);
        }
    }

    protected void getPackages(Configuration conf, String packageType)
        throws ConfigurationException {
        packages = new Vector();
        packages.addElement("");
        final Configuration[] pkgConfs = conf.getChildren(packageType);
        for (int i = 0; i < pkgConfs.length; i++) {
            Configuration c = pkgConfs[i];
            String packageName = c.getValue();
            if (!packageName.endsWith(".")) {
                packageName += ".";
            }
            packages.addElement(packageName);
        }
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager sm) throws ServiceException {
        serviceManager = new DefaultServiceManager(sm);
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        mailetContext = (MailetContext) serviceManager.lookup("org.apache.mailet.MailetContext");
    }
        
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public abstract void configure(Configuration arg0) throws ConfigurationException;

}
