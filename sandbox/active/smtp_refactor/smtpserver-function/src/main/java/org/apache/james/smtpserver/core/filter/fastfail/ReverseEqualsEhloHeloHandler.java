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

import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.mailet.MailAddress;


import java.net.UnknownHostException;


public class ReverseEqualsEhloHeloHandler extends ResolvableEhloHeloHandler {
    
    @Override
	public String onEhlo(SMTPSession session, String heloName) {
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
                if (!heloName.equals(reverse)) {
                    badHelo = true;
                }
            } catch (UnknownHostException e) {
                badHelo = true;
            }

            // bad EHLO/HELO
            if (badHelo)
                session.getState().put(BAD_EHLO_HELO, "true");
        }
        return null;
	}

	@Override
	public String onHelo(SMTPSession session, String heloName) {
		return onEhlo(session, heloName);
	}

	@Override
	public String onRcpt(SMTPSession session, MailAddress rcpt) {
		String responseString = null;
		if (session.getState().containsKey(BAD_EHLO_HELO)) {
			responseString = "501 "+ DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_ARG)
	            + " Provided EHLO/HELO " + session.getState().get(SMTPSession.CURRENT_HELO_NAME) + " not equal reverse of "
	                    + session.getRemoteIPAddress();
		}
		
        return responseString;
	}

}
