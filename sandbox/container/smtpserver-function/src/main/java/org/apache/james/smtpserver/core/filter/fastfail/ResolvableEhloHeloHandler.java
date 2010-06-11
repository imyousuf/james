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

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.junkscore.JunkScore;
import org.apache.mailet.MailAddress;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This CommandHandler can be used to reject not resolvable EHLO/HELO
 */
public class ResolvableEhloHeloHandler extends AbstractJunkHandler implements
        CommandHandler, Configurable, Serviceable {

    public final static String BAD_EHLO_HELO = "BAD_EHLO_HELO";

    protected boolean checkAuthNetworks = false;

    private boolean checkAuthUsers = false;

    protected DNSService dnsServer = null;

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

        Configuration configAuthUser = handlerConfiguration.getChild(
                "checkAuthUsers", false);
        if (configAuthUser != null) {
            setCheckAuthUsers(configAuthUser.getValueAsBoolean(false));
        }
        
        super.configure(handlerConfiguration);
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager serviceMan) throws ServiceException {
        setDnsServer((DNSService) serviceMan.lookup(DNSService.ROLE));
    }

    /**
     * Set to true if AuthNetworks should be included in the EHLO/HELO check
     * 
     * @param checkAuthNetworks
     *            Set to true to enable
     */
    public void setCheckAuthNetworks(boolean checkAuthNetworks) {
        this.checkAuthNetworks = checkAuthNetworks;
    }

    /**
     * Set to true if Auth users should be included in the EHLO/HELO check
     * 
     * @param checkAuthUsers
     *            Set to true to enable
     */
    public void setCheckAuthUsers(boolean checkAuthUsers) {
        this.checkAuthUsers = checkAuthUsers;
    }

    /**
     * Set the DNSService
     * 
     * @param dnsServer
     *            The DNSService
     */
    public void setDnsServer(DNSService dnsServer) {
        this.dnsServer = dnsServer;
    }

    /**
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        String argument = session.getCommandArgument();
        String command = session.getCommandName();
        if (command.equals("HELO")
                || command.equals("EHLO")) {
            checkEhloHelo(session, argument);
        } else if (command.equals("RCPT")) {
            doProcessing(session);
        }
    }

    /**
     * Check if EHLO/HELO is resolvable
     * 
     * @param session
     *            The SMTPSession
     * @param argument
     *            The argument
     */
    protected void checkEhloHelo(SMTPSession session, String argument) {
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
                session.getState().put(BAD_EHLO_HELO, "true");
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
        implCommands.add("RCPT");

        return implCommands;
    }

    /**
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#check(org.apache.james.smtpserver.SMTPSession)
     */
    protected boolean check(SMTPSession session) {
    
        MailAddress rcpt = (MailAddress) session.getState().get(
                SMTPSession.CURRENT_RECIPIENT);

        // not reject it
        if (session.getState().get(BAD_EHLO_HELO) == null
                || rcpt.getUser().equalsIgnoreCase("postmaster")
                || rcpt.getUser().equalsIgnoreCase("abuse"))
            return false;

        // Check if the client was authenticated
        if (!(session.isAuthRequired() && session.getUser() != null && !checkAuthUsers)) {
            return true;
        }
        return false;
    }


    /**
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#getJunkScore(org.apache.james.smtpserver.SMTPSession)
     */
    protected JunkScore getJunkScore(SMTPSession session) {
        return (JunkScore) session.getConnectionState().get(JunkScore.JUNK_SCORE_SESSION);
    }
    
    /**
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#getJunkHandlerData(org.apache.james.smtpserver.SMTPSession)
     */
    public JunkHandlerData getJunkHandlerData(SMTPSession session) {
        JunkHandlerData data = new JunkHandlerData();
        
        data.setJunkScoreLogString("Provided EHLO/HELO " + session.getState().get(SMTPSession.CURRENT_HELO_NAME) + " can not resolved. Add junkScore: " + getScore());
        data.setRejectLogString("501 " + DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_ARG)
                + " Provided EHLO/HELO " + session.getState().get(SMTPSession.CURRENT_HELO_NAME) + " can not resolved");
    
        
        data.setRejectResponseString("501 " + DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_ARG)
                + " Provided EHLO/HELO " + session.getState().get(SMTPSession.CURRENT_HELO_NAME) + " can not resolved");

        data.setScoreName("ResolvableEhloHeloCheck");
        return data;
    }

}
