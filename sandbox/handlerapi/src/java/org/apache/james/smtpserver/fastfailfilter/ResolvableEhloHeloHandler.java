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

package org.apache.james.smtpserver.fastfailfilter;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.DNSServer;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.dsn.DSNStatus;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

public class ResolvableEhloHeloHandler extends AbstractLogEnabled
        implements CommandHandler, Configurable, Serviceable {

    private boolean checkAuthNetworks = false;

    private DNSServer dnsServer = null;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration)
            throws ConfigurationException {
        Configuration configRelay = handlerConfiguration.getChild(
                "checkAuthNetworks", false);
        if (configRelay != null) {
            setCheckAuthNetworks(configRelay.getValueAsBoolean(false));
        }
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager serviceMan) throws ServiceException {
        setDnsServer((DNSServer) serviceMan.lookup(DNSServer.ROLE));
    }

    /**
     * Set to true if AuthNetworks should be included in the EHLO check
     * 
     * @param checkAuthNetworks
     *            Set to true to enable
     */
    public void setCheckAuthNetworks(boolean checkAuthNetworks) {
        this.checkAuthNetworks = checkAuthNetworks;
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
     * @see org.apache.james.smtpserver.fastfailfilter.HeloFilterHandler#onEhloCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        String argument = session.getCommandArgument();
        String responseString = null;

        /**
         * don't check if the ip address is allowed to relay. Only check if it
         * is set in the config.
         */
        if (!session.isRelayingAllowed() || checkAuthNetworks) {
            // try to resolv the provided helo. If it can not resolved do not
            // accept it.
            try {
                dnsServer.getByName(argument);
            } catch (UnknownHostException e) {
                responseString = "501 "
                        + DSNStatus.getStatus(DSNStatus.PERMANENT,
                                DSNStatus.DELIVERY_INVALID_ARG)
                        + " Provided EHLO/HELO " + argument
                        + " can not resolved";
                session.writeResponse(responseString);
                getLogger().info(responseString);

                // After this filter match we should not call any other handler!
                session.setStopHandlerProcessing(true);
            }
        }
    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("EHLO");
        implCommands.add("HELO");
        
        return implCommands;
    }
 
}
