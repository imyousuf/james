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



package org.apache.james.smtpserver.core.filter.fastfail;

import java.util.Collection;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.dnsservice.TemporaryResolutionException;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.core.PostMailListener;
import org.apache.mailet.MailAddress;

public class ValidSenderDomainHandler
    extends AbstractLogEnabled
    implements Configurable, Serviceable, PostMailListener {
    
    private boolean checkAuthClients = false;
    
    private DNSService dnsServer = null;

    
    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration) throws ConfigurationException {
        
        Configuration configRelay = handlerConfiguration.getChild("checkAuthClients",false);
        if(configRelay != null) {
            setCheckAuthClients(configRelay.getValueAsBoolean(false));
        }
    }
    
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager serviceMan) throws ServiceException {
        setDnsServer((DNSService) serviceMan.lookup(DNSService.ROLE));
    }
    
    /**
     * Set the DnsServer
     * 
     * @param dnsServer The DnsServer
     */
    public void setDnsServer(DNSService dnsServer) {
        this.dnsServer = dnsServer;
    }
    
    /**
     * Enable checking of authorized clients
     * 
     * @param checkAuthClients Set to true to enable
     */
    public void setCheckAuthClients(boolean checkAuthClients) {
        this.checkAuthClients = checkAuthClients;
    }
    
    /**
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.core.PostMailListener#onMail(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.MailAddress)
     */
	public String onMail(SMTPSession session, MailAddress senderAddress) {         
	        // null sender so return
	        if (senderAddress == null) return null;
	            
	        /**
	         * don't check if the ip address is allowed to relay. Only check if it is set in the config. 
	         */
	        if (checkAuthClients || !session.isRelayingAllowed()) {
	            Collection records = null;
	            
	                
	            // try to resolv the provided domain in the senderaddress. If it can not resolved do not accept it.
	            try {
	                records = dnsServer.findMXRecords(senderAddress.getHost());
	            } catch (TemporaryResolutionException e) {
	                // TODO: Should we reject temporary ?
	            }
	        
	            if (records == null || records.size() == 0) {
	                return "501 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.ADDRESS_SYNTAX_SENDER)+ " sender " + senderAddress + " contains a domain with no valid MX records";
	            }
	        }
	        return null;
	}
}
