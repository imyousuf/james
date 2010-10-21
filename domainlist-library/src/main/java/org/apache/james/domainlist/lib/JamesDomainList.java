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

package org.apache.james.domainlist.lib;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.Disposable;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.services.InstanceFactory;

/**
 * 
 *
 */
public class JamesDomainList implements DomainList, LogEnabled, Configurable{

    private InstanceFactory instanceFactory;
    private HierarchicalConfiguration config;
    private Log log;
    private DomainList domainList;

    @Resource(name="instanceFactory")
    public void setInstanceFactory(InstanceFactory instanceFactory) {
        this.instanceFactory = instanceFactory;
    }

    @PostConstruct
    public void init() throws Exception {
        HierarchicalConfiguration conf = config.configurationAt("domainlist");

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String repName = conf.getString("[@name]", null);
        String repClass = conf.getString("[@class]");

        if (repName == null) {
            repName = repClass;
        }

        if (log.isDebugEnabled()) {
            log.debug("Starting " + repClass);
        }

        domainList = (DomainList) instanceFactory.newInstance(loader
                .loadClass(repClass), log, conf);

        if (log.isInfoEnabled()) {
            StringBuffer logBuffer = new StringBuffer(64).append("Bean  ")
                    .append(repName).append(" started.");
            log.info(logBuffer.toString());
        }

    }
    
    @PreDestroy
    public void destroy() {
        if (domainList != null) {
            if (domainList instanceof Disposable) {
                ((Disposable) domainList).dispose();
            }
        }
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.api.domainlist.ManageableDomainList#addDomain(java.lang.String)
     */
    public boolean addDomain(String domain) {
        return domainList.addDomain(domain);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.domainlist.ManageableDomainList#removeDomain(java.lang.String)
     */
    public boolean removeDomain(String domain) {
        return domainList.removeDomain(domain);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.domainlist.DomainList#containsDomain(java.lang.String)
     */
    public boolean containsDomain(String domain) {
        return domainList.containsDomain(domain);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.domainlist.DomainList#getDomains()
     */
    public String[] getDomains() {
        return domainList.getDomains();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.log = log;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        this.config = config;
    }

}
