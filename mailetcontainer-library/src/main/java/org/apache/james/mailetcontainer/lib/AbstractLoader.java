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


package org.apache.james.mailetcontainer.lib;
import java.util.List;
import java.util.Vector;

import javax.annotation.Resource;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.services.InstanceFactory;
import org.apache.mailet.MailetContext;
import org.apache.mailet.MailetException;

/**
 * Common services for loaders.
 */
public abstract class AbstractLoader implements LogEnabled, Configurable {

    /**
     * The list of packages that may contain Mailets or matchers
     */
    protected Vector<String> packages;

    /**
     * Mailet context
     */
    protected MailetContext mailetContext;

    private Log logger;

    protected InstanceFactory factory;

   
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public final void setLog(Log logger) {
        this.logger = logger;
    }
    
    /**
     * Set the MailetContext
     * 
     * @param mailetContext the MailetContext
     */
    @Resource(name="mailetcontext") 
    public void setMailetContext(MailetContext mailetContext) {
        this.mailetContext = mailetContext;
    }
    
    @Resource(name="instanceFactory") 
    public void setFactory(InstanceFactory factory) {
        this.factory = factory;
    }

    protected Log getLogger() {
        return logger;
    }
    

    @SuppressWarnings("unchecked")
    protected void getPackages(HierarchicalConfiguration conf, String packageType)
        throws ConfigurationException {
        packages = new Vector<String>();
        packages.addElement("");
        final List<String> pkgConfs = conf.getList(packageType);
        for (int i = 0; i < pkgConfs.size(); i++) {
            String packageName = pkgConfs.get(i);
            if (!packageName.endsWith(".")) {
                packageName += ".";
            }
            packages.addElement(packageName);
        }
    }
    
            
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
