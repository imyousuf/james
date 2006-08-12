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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
import org.apache.james.util.NetMatcher;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.MailAddress;

/**
 * Reject email with an MX which is 127.0.0.1. This class can be used to reject
 * email to bogus MX which is send from a authorized user or an authorized
 * network.
 */
public class ValidRcptMX extends AbstractLogEnabled implements CommandHandler,
	Serviceable, Configurable {

    private DNSServer dnsServer = null;

    private static final String LOCALHOST = "localhost";

    private NetMatcher bNetwork = null;

    public void configure(Configuration arg0) throws ConfigurationException {

	Configuration[] badMX = arg0.getChildren("invalidMXNetworks");

	if (badMX.length != 0) {

	    Collection bannedNetworks = new ArrayList();

	    for (int i = 0; i < badMX.length; i++) {
		String network = badMX[i].getValue(null);

		if (network != null) {
		    bannedNetworks.add(network);
		}
	    }

	    setBannedNetworks(bannedNetworks, dnsServer);

	    getLogger().info("Invalid MX Networks: " + bNetwork.toString());

	} else {
	    throw new ConfigurationException(
		    "Please configure at least on invalid MX network");
	}
    }

    public void setBannedNetworks(Collection networks, DNSServer dnsServer) {
	bNetwork = new NetMatcher(networks, dnsServer) {
	    protected void log(String s) {
		getLogger().debug(s);
	    }

	};
    }

    /**
         * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
         */
    public void service(ServiceManager arg0) throws ServiceException {
	setDNSServer((DNSServer) arg0.lookup(DNSServer.ROLE));
    }

    /**
         * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
         */
    public Collection getImplCommands() {
	Collection c = new ArrayList();
	c.add("RCPT");
	return c;
    }

    /**
         * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
         */
    public void onCommand(SMTPSession session) {
	doRCPT(session);
    }

    /**
         * Set the DNSServer
         * 
         * @param dnsServer
         *                The dnsServer
         */
    public void setDNSServer(DNSServer dnsServer) {
	this.dnsServer = dnsServer;
    }

    private void doRCPT(SMTPSession session) {
	MailAddress rcpt = (MailAddress) session.getState().get(
		SMTPSession.CURRENT_RECIPIENT);

	String domain = rcpt.getHost();

	// Email should be deliver local
	if (domain.equals(LOCALHOST))
	    return;

	Iterator mx = dnsServer.findMXRecords(domain).iterator();

	if (mx.hasNext()) {
	    while (mx.hasNext()) {
		String mxRec = mx.next().toString();

		try {
		    String ip = dnsServer.getByName(mxRec).getHostAddress();

		    // Check for invalid MX
		    if (bNetwork.matchInetNetwork(ip)) {
			String response = "Invalid MX " + ip + " for domain "
				+ rcpt.getHost();
			String responseString = "530"
				+ DSNStatus.getStatus(DSNStatus.PERMANENT,
					DSNStatus.SECURITY_AUTH) + " "
				+ response;
			getLogger().debug(response + ". Reject email");
			session.writeResponse(responseString);
			return;
		    }
		} catch (UnknownHostException e) {
		    // Ignore this
		}
	    }
	}
    }
}
