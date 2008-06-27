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

import java.util.ArrayList;
import java.util.Collection;

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
import org.apache.mailet.MailAddress;

public class ValidSenderDomainHandler
    extends AbstractLogEnabled
    implements CommandHandler, Configurable, Serviceable {
    
    private boolean checkAuthClients = false;
    
    private DNSServer dnsServer = null;
    
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
        setDnsServer((DNSServer) serviceMan.lookup(DNSServer.ROLE));
    }
    
    /**
     * Set the DnsServer
     * 
     * @param dnsServer The DnsServer
     */
    public void setDnsServer(DNSServer dnsServer) {
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
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
    String response = doMAIL(session);
    if (response == null) {
        // call the next handler in chain
        session.doChain();

    } else {
        // store the response
        session.getSMTPResponse().setRawSMTPResponse(response);
    }
    }

    private String doMAIL(SMTPSession session) {

    String responseString = null;
    MailAddress senderAddress = (MailAddress) session.getState().get(
        SMTPSession.SENDER);

    // null sender so return
    if (senderAddress == null)
        return null;

    /**
         * don't check if the ip address is allowed to relay. Only check if it
         * is set in the config.
         */
    if (checkAuthClients || !session.isRelayingAllowed()) {
        // try to resolv the provided domain in the
        // senderaddress. If it can not resolved do not accept
        // it.

        Collection records = dnsServer.findMXRecords(senderAddress
            .getHost());

        if (records == null || records.size() == 0) {
        responseString = "501 "
            + DSNStatus.getStatus(DSNStatus.PERMANENT,
                DSNStatus.ADDRESS_SYNTAX_SENDER) + " sender "
            + senderAddress
            + " contains a domain with no valid MX records";
        getLogger().info(responseString);
        }

    }
    return responseString;
    }
    
    /**
         * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
         */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("MAIL");
        
        return implCommands;
    }
}
