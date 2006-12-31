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
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.EhloHook;
import org.apache.james.smtpserver.hook.HeloHook;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.RcptHook;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;

import org.apache.mailet.MailAddress;

import java.net.UnknownHostException;


/**
 * This CommandHandler can be used to reject not resolvable EHLO/HELO
 */
public class ResolvableEhloHeloHandler extends AbstractLogEnabled implements Configurable, Serviceable, RcptHook, EhloHook, HeloHook {

    public final static String BAD_EHLO_HELO = "BAD_EHLO_HELO";

    protected boolean checkAuthNetworks = false;

    private boolean checkAuthUsers = false;

    protected DNSServer dnsServer = null;

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
     * Set the DNSServer
     * 
     * @param dnsServer
     *            The DNSServer
     */
    public void setDnsServer(DNSServer dnsServer) {
        this.dnsServer = dnsServer;
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
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#check(org.apache.james.smtpserver.SMTPSession)
     */
    protected boolean check(SMTPSession session,MailAddress rcpt) {

        // not reject it
        if (session.getState().get(BAD_EHLO_HELO) == null)
            return false;

        // Check if the client was authenticated
        if (!(session.isAuthRequired() && session.getUser() != null && !checkAuthUsers)) {
            return true;
        }
        return false;
    }

    /**
     * @see org.apache.james.smtpserver.hook.RcptHook#doRcpt(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
     */
    public HookResult doRcpt(SMTPSession session, MailAddress sender, MailAddress rcpt) {
        if (check(session,rcpt)) {
            return new HookResult(HookReturnCode.DENY,SMTPRetCode.SYNTAX_ERROR_ARGUMENTS,DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_ARG)
                    + " Provided EHLO/HELO " + session.getState().get(SMTPSession.CURRENT_HELO_NAME) + " can not resolved.");
        } else {
            return new HookResult(HookReturnCode.DECLINED);
        }
    }

    /**
     * @see org.apache.james.smtpserver.hook.EhloHook#doEhlo(org.apache.james.smtpserver.SMTPSession, java.lang.String)
     */
    public HookResult doEhlo(SMTPSession session, String helo) {
        return doHelo(session,helo);
    }

    /**
     * @see org.apache.james.smtpserver.hook.HeloHook#doHelo(org.apache.james.smtpserver.SMTPSession, java.lang.String)
     */
    public HookResult doHelo(SMTPSession session, String helo) {
        checkEhloHelo(session, helo);
        return new HookResult(HookReturnCode.DECLINED);
    }

}
