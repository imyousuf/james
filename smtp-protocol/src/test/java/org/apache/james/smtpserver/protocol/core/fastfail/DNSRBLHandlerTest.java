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


package org.apache.james.smtpserver.protocol.core.fastfail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.james.smtpserver.protocol.BaseFakeDNSService;
import org.apache.james.smtpserver.protocol.BaseFakeSMTPSession;
import org.apache.james.smtpserver.protocol.DNSService;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.mailet.MailAddress;

public class DNSRBLHandlerTest extends TestCase {

    private DNSService mockedDnsServer;

    private SMTPSession mockedSMTPSession;

    private String remoteIp = "127.0.0.2";

    private boolean relaying = false;   
    
    public static final String RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME = "org.apache.james.smtpserver.rbl.blocklisted";
    
    public static final String RBL_DETAIL_MAIL_ATTRIBUTE_NAME = "org.apache.james.smtpserver.rbl.detail";

    protected void setUp() throws Exception {
        super.setUp();
        setupMockedDnsServer();
        setRelayingAllowed(false);
    }

    /**
     * Set the remoteIp
     * 
     * @param remoteIp The remoteIP to set
     */
    private void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    /**
     * Set relayingAllowed
     * 
     * @param relaying true or false
     */
    private void setRelayingAllowed(boolean relaying) {
        this.relaying = relaying;
    }

    /**
     * Setup the mocked dnsserver
     *
     */
    private void setupMockedDnsServer() {
        mockedDnsServer  = new BaseFakeDNSService() {

            public Collection<String> findTXTRecords(String hostname) {
                List<String> res = new ArrayList<String>();
                if (hostname == null) {
                    return res;
                }
                ;
                if ("2.0.0.127.bl.spamcop.net.".equals(hostname)) {
                    res.add("Blocked - see http://www.spamcop.net/bl.shtml?127.0.0.2");
                }
                return res;
            }

            public InetAddress getByName(String host)
                    throws UnknownHostException {
                if ("2.0.0.127.bl.spamcop.net.".equals(host)) {
                    return InetAddress.getByName("127.0.0.1");
                } else if ("3.0.0.127.bl.spamcop.net.".equals(host)) {
                    return InetAddress.getByName("127.0.0.1");
                } else if ("1.0.168.192.bl.spamcop.net.".equals(host)) {
                    throw new UnknownHostException(host);
                }
                throw new UnsupportedOperationException("getByName("+host+") not implemented in DNSRBLHandlerTest mock");
            }
        };
        
    }

    /**
     * Setup mocked smtpsession
     */
    private void setupMockedSMTPSession(final MailAddress rcpt) {
        mockedSMTPSession = new BaseFakeSMTPSession() {
            HashMap<String,Object> state = new HashMap<String,Object>();
            HashMap<String,Object> connectionState = new HashMap<String,Object>();
            
            public String getRemoteIPAddress() {
                return remoteIp;
            }

            public Map<String,Object> getState() {
                return state;
            }

            public boolean isRelayingAllowed() {
                return relaying;
            }

            public boolean isAuthSupported() {
                return false;
            }

            public int getRcptCount() {
                return 0;
            }

            public Map<String,Object> getConnectionState() {       
                return connectionState;
            }

        };
    }

    // ip is blacklisted and has txt details
    public void testBlackListedTextPresent() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();
       
        setupMockedSMTPSession(new MailAddress("any@domain"));
        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertEquals("Details","Blocked - see http://www.spamcop.net/bl.shtml?127.0.0.2",
               mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip is blacklisted and has txt details but we don'T want to retrieve the txt record
    public void testGetNoDetail() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();
        setupMockedSMTPSession(new MailAddress("any@domain"));
        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(false);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip is allowed to relay
    public void testRelayAllowed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();
        setRelayingAllowed(true);
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip not on blacklist
    public void testNotBlackListed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        setRemoteIp("192.168.0.1");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip on blacklist without txt details
    public void testBlackListedNoTxt() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        setRemoteIp("127.0.0.3");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull(mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip on whitelist
    public void testWhiteListed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        setRemoteIp("127.0.0.2");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSService(mockedDnsServer);

        rbl.setWhitelist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull(mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }
   

}
