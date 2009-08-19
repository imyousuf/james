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
import java.util.Vector;

import javax.annotation.Resource;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.api.kernel.LoaderService;
import org.apache.mailet.MailetContext;
import org.apache.mailet.MailetException;

/**
 * Common services for loaders.
 */
public abstract class AbstractLoader extends AbstractLogEnabled implements Configurable {

    /**
     * The list of packages that may contain Mailets or matchers
     */
    protected Vector<String> packages;

    /**
     * Mailet context
     */
    protected MailetContext mailetContext;

    private LoaderService loaderService;

    
    /**
     * Gets the loader service used by this instance.
     * @return the loaderService 
     */
    public final LoaderService getLoaderService() {
        return loaderService;
    }

    /**
     * Sets the loader service used by this instance.
     * @param loaderService the loaderService to set, not null
     */
    @Resource(name="org.apache.james.LoaderService")
    public final void setLoaderService(LoaderService loaderService) {
        this.loaderService = loaderService;
    }

    /**
     * Set the MailetContext
     * 
     * @param mailetContext the MailetContext
     */
 // Pheonix used to play games with service names
 // TODO: Support type based injection
    @Resource(name="James") 
    public void setMailetContext(MailetContext mailetContext) {
        this.mailetContext = mailetContext;
    }

    protected Object load(String className) throws ClassNotFoundException {
        final Object newInstance = loaderService.load(Thread.currentThread().getContextClassLoader().loadClass(className));
        return newInstance;
    }

    protected void getPackages(Configuration conf, String packageType)
        throws ConfigurationException {
        packages = new Vector<String>();
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
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public abstract void configure(Configuration arg0) throws ConfigurationException;
    
    /**
     * Gets a human readable description of the loader.
     * Used for messages.
     * @return not null
     */
    protected abstract String getDisplayName();

    /**
     * Constructs an appropriate exception with an appropriate message
     * @param name not null
     * @return not null
     */
    protected ClassNotFoundException classNotFound(String name) throws ClassNotFoundException {
        final StringBuilder builder =
            new StringBuilder(128)
                .append("Requested ")
                .append(getDisplayName())
                .append(" not found: ")
                .append(name)
                .append(".  Package searched: ");
        for (final String packageName:packages) {
            builder.append(packageName);
            builder.append(" ");
        }
        return new ClassNotFoundException(builder.toString());
    }

    /**
     * Constructs an appropriate exception with an appropriate message.
     * @param name not null
     * @param e not null
     * @return not null
     */
    protected MailetException loadFailed(String name, Exception e) {
        final StringBuilder builder =
            new StringBuilder(128).append("Could not load ").append(getDisplayName())
                .append(" (").append(name).append(")");
        final MailetException mailetException = new MailetException(builder.toString(), e);
        return mailetException;
    }

}
