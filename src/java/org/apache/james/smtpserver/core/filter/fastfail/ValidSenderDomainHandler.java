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
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.DNSServer;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.MailAddress;

public class ValidSenderDomainHandler
    extends AbstractJunkHandler
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
        
        super.configure(handlerConfiguration);
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
        doProcessing(session);
    }
    
    /**
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#check(org.apache.james.smtpserver.SMTPSession)
     */
    protected boolean check(SMTPSession session) {
        MailAddress senderAddress = (MailAddress) session.getState().get(SMTPSession.SENDER);
            
        // null sender so return
        if (senderAddress == null) return false;
            
        /**
         * don't check if the ip address is allowed to relay. Only check if it is set in the config. 
         */
        if (checkAuthClients || !session.isRelayingAllowed()) {
            Collection records;
            
                
            // try to resolv the provided domain in the senderaddress. If it can not resolved do not accept it.
            records = dnsServer.findMXRecords(senderAddress.getHost());
            if (records == null || records.size() == 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("MAIL");
        
        return implCommands;
    }

    /**
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#getJunkScoreLogString(org.apache.james.smtpserver.SMTPSession)
     */
    protected String getJunkScoreLogString(SMTPSession session) {
       MailAddress senderAddress = (MailAddress) session.getState().get(SMTPSession.SENDER);
        String response = "Sender " + senderAddress + " contains a domain with no valid MX records. Add Junkscore: " + getScore();
        return response;
    }
    
    /**
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#getRejectLogString(org.apache.james.smtpserver.SMTPSession)
     */
    protected String getRejectLogString(SMTPSession session) {
        return getResponseString(session);
    }
    
    /**
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#getResponseString(org.apache.james.smtpserver.SMTPSession)
     */
    protected String getResponseString(SMTPSession session) {
        MailAddress senderAddress = (MailAddress) session.getState().get(SMTPSession.SENDER);
        String response = "501 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.ADDRESS_SYNTAX_SENDER)+ " sender " + senderAddress + " contains a domain with no valid MX records";
        return response;
    }

    /**
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#getScoreName()
     */
    protected String getScoreName() {
        return "ValidSenderDomainCheck";
    }
}
