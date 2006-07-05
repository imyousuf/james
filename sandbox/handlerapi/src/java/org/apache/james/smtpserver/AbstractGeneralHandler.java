/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
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

package org.apache.james.smtpserver;

import java.util.List;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.DNSServer;

/**
 * Custom CommandHandlers must extend this class.
 */
public abstract class AbstractGeneralHandler extends AbstractLogEnabled implements Configurable,Serviceable,Initializable{

    /**
     * If set to true all handler processing is stopped (fastfail)
     */
    private boolean stopHandlerProcessing = false;

    private DataSourceSelector dataSource;

    private DNSServer dnsServer;
    
    /**
     * Method to set if a after the handler no other command handlers should
     * processed
     * 
     * @param stopHandlerProcessing
     *            true or false
     */
    public void setStopHandlerProcessing(boolean stopHandlerProcessing) {
        this.stopHandlerProcessing = stopHandlerProcessing;
    }

    /**
     * Return if the processing of other commandHandlers should be done
     * 
     * @return true or false
     */
    public boolean stopHandlerProcessing() {
        return stopHandlerProcessing;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration)
            throws ConfigurationException {
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        // only for overriding
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager serviceMan) throws ServiceException {
        setDataSourceSelector((DataSourceSelector) serviceMan
                .lookup(DataSourceSelector.ROLE));

        setDnsServer((DNSServer) serviceMan.lookup(DNSServer.ROLE));
    }

    /**
     * Set the DNSServer
     * 
     * @param dnsServer
     *            The DNSServer
     */
    public void setDnsServer(DNSServer dnsServer) {
        this.dnsServer = dnsServer;
    }

    /**
     * Return the DnsServer
     * 
     * @return DNSServer
     */
    public DNSServer getDnsServer() {
        return dnsServer;
    }

    /**
     * Set the datasource
     * 
     * @param datasource
     *            the datasource
     */
    public void setDataSourceSelector(DataSourceSelector dataSource) {

        this.dataSource = dataSource;
    }

    /**
     * Return the DataSourceSelector
     * 
     * @return DataSourceSelector
     */
    public DataSourceSelector getDataSourceSelector() {
        return dataSource;
    }
}
