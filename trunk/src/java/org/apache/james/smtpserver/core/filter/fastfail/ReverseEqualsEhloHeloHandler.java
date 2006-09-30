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
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.DNSServer;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.MailAddress;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

public class ReverseEqualsEhloHeloHandler extends AbstractLogEnabled implements
        CommandHandler, Configurable, Serviceable {

    public final static String BAD_EHLO_HELO = "BAD_EHLO_HELO";

    private boolean checkAuthNetworks = false;

    private boolean checkAuthUsers = false;

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

        Configuration configAuthUser = handlerConfiguration.getChild(
                "checkAuthUsers", false);
        if (configAuthUser != null) {
            setCheckAuthUsers(configAuthUser.getValueAsBoolean(false));
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
     * Set to true if Auth users should be included in the EHLO/HELO check
     * 
     * @param checkAuthUsers
     *            Set to true to enable
     */
    public void setCheckAuthUsers(boolean checkAuthUsers) {
        this.checkAuthUsers = checkAuthUsers;
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
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        String argument = session.getCommandArgument();
        String command = session.getCommandName();
        if (command.equals("HELO") || command.equals("EHLO")) {
            checkEhloHelo(session, argument);
        } else if (command.equals("RCPT")) {
            reject(session, argument);
        }
    }

    /**
     * Method which get called on HELO/EHLO
     * 
     * @param session The SMTPSession
     * @param argument The argument
     */
    private void checkEhloHelo(SMTPSession session, String argument) {
        /**
         * don't check if the ip address is allowed to relay. Only check if it
         * is set in the config. ed.
         */
        if (!session.isRelayingAllowed() || checkAuthNetworks) {
            boolean badHelo = false;
            try {
                // get reverse entry
                String reverse = dnsServer.getHostName(dnsServer.getByName(
                        session.getRemoteIPAddress()));
                if (!argument.equals(reverse)) {
                    badHelo = true;
                }
            } catch (UnknownHostException e) {
                badHelo = true;
            }

            // bad EHLO/HELO
            if (badHelo)
                session.getState().put(BAD_EHLO_HELO, "true");
        }
    }

    /**
     * Method which get called on RCPT
     * 
     * @param session The SMTPSession
     * @param argument The argument
     */
    private void reject(SMTPSession session, String argument) {
        MailAddress rcpt = (MailAddress) session.getState().get(
                SMTPSession.CURRENT_RECIPIENT);

        // not reject it
        if (session.getState().get(BAD_EHLO_HELO) == null
                || rcpt.getUser().equalsIgnoreCase("postmaster")
                || rcpt.getUser().equalsIgnoreCase("abuse"))
            return;

        // Check if the client was authenticated
        if (!(session.isAuthRequired() && session.getUser() != null && !checkAuthUsers)) {
            String responseString = "501 "
                    + DSNStatus.getStatus(DSNStatus.PERMANENT,
                            DSNStatus.DELIVERY_INVALID_ARG) + " Provided EHLO "
                    + session.getState().get(SMTPSession.CURRENT_HELO_NAME) + " not equal reverse of "
                    + session.getRemoteIPAddress();

            session.writeResponse(responseString);
            getLogger().info(responseString);

            // After this filter match we should not call any other handler!
            session.setStopHandlerProcessing(true);
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

}
