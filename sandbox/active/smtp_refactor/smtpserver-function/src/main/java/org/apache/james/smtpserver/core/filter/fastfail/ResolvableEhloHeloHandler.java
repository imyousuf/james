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

import java.net.UnknownHostException;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.core.PostEhloListener;
import org.apache.james.smtpserver.core.PostHeloListener;
import org.apache.james.smtpserver.core.PostRcptListener;
import org.apache.james.smtpserver.junkscore.JunkScore;
import org.apache.mailet.MailAddress;

/**
 * This CommandHandler can be used to reject not resolvable EHLO/HELO
 */
public class ResolvableEhloHeloHandler extends AbstractLogEnabled implements
        Configurable, Serviceable,PostEhloListener,PostHeloListener,PostRcptListener{

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
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#getJunkScore(org.apache.james.smtpserver.SMTPSession)
     */
    protected JunkScore getJunkScore(SMTPSession session) {
        return (JunkScore) session.getConnectionState().get(JunkScore.JUNK_SCORE_SESSION);
    }

    /**
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.core.PostEhloListener#onEhlo(org.apache.james.smtpserver.SMTPSession, java.lang.String)
     */
	public String onEhlo(SMTPSession session, String heloName) {
		 /**
         * don't check if the ip address is allowed to relay. Only check if it
         * is set in the config.
         */
        if (!session.isRelayingAllowed() || checkAuthNetworks) {
            // try to resolv the provided helo. If it can not resolved do not
            // accept it.
            try {
                dnsServer.getByName(heloName);
            } catch (UnknownHostException e) {
                session.getState().put(BAD_EHLO_HELO, "true");
            }
        }
        return null;
	}

	/**
	 * (non-Javadoc)
	 * @see org.apache.james.smtpserver.core.PostHeloListener#onHelo(org.apache.james.smtpserver.SMTPSession, java.lang.String)
	 */
	public String onHelo(SMTPSession session, String heloName) {
		return onEhlo(session, heloName);
	}

	/**
	 * (non-Javadoc)
	 * @see org.apache.james.smtpserver.core.PostRcptListener#onRcpt(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.MailAddress)
	 */
	public String onRcpt(SMTPSession session, MailAddress rcpt) {
        // not reject it
        if (session.getState().get(BAD_EHLO_HELO) == null
                || rcpt.getLocalPart().equalsIgnoreCase("postmaster")
                || rcpt.getLocalPart().equalsIgnoreCase("abuse"))
            return null;

        // Check if the client was authenticated
        if (!(session.isAuthRequired() && session.getUser() != null && !checkAuthUsers)) {
            return "501 " + DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_ARG)
                + " Provided EHLO/HELO " + session.getState().get(SMTPSession.CURRENT_HELO_NAME) + " can not resolved";
        }
		return null;
	}

}
