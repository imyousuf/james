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



package org.apache.james.transport.matchers;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.mail.MessagingException;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.api.dnsservice.DNSServer;
import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;

/**
 * Checks the network IP address of the sending server against a
 * blacklist of spammers.  There are 3 lists that support this...
 * <ul>
 * <li><b>blackholes.mail-abuse.org</b>: Rejected - see  http://www.mail-abuse.org/rbl/
 * <li><b>dialups.mail-abuse.org</b>: Dialup - see http://www.mail-abuse.org/dul/
 * <li><b>relays.mail-abuse.org</b>: Open spam relay - see http://www.mail-abuse.org/rss/
 * </ul>
 *
 * Example:
 * &lt;mailet match="InSpammerBlacklist=blackholes.mail-abuse.org." class="ToProcessor"&gt;
 *   &lt;processor&gt;spam&lt;/processor&gt;
 * &lt;/mailet&gt;
 *
 */
public class InSpammerBlacklist extends GenericMatcher {
    private String network = null;
    
    private DNSServer dnsServer;

    public void init() throws MessagingException {
        network = getCondition();
        
        // check if the needed condition was given
        if (network == null) throw new MessagingException("Please configure a blacklist");
        
        ServiceManager compMgr = (ServiceManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        
        try {
            // Instantiate DNSService
            dnsServer = (DNSServer) compMgr.lookup(DNSServer.ROLE);
        } catch (ServiceException cnfe) {
            log("Failed to retrieve DNSService" + cnfe.getMessage());
        } catch (Exception e) {
            log("Failed to retrieve DNSService:" + e.getMessage());
        }

    }

    public Collection match(Mail mail) {
        String host = mail.getRemoteAddr();
        try {
            //Have to reverse the octets first
            StringBuffer sb = new StringBuffer();
            StringTokenizer st = new StringTokenizer(host, " .", false);

            while (st.hasMoreTokens()) {
                sb.insert(0, st.nextToken() + ".");
            }

            //Add the network prefix for this blacklist
            sb.append(network);

            //Try to look it up
            dnsServer.getByName(sb.toString());

            //If we got here, that's bad... it means the host
            //  was found in the blacklist
            return mail.getRecipients();
        } catch (UnknownHostException uhe) {
            //This is good... it's not on the list
            return null;
        }
    }
}
