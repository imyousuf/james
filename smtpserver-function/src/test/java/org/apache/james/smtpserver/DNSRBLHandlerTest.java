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


package org.apache.james.smtpserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.ParseException;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.services.AbstractDNSServer;
import org.apache.james.smtpserver.core.filter.fastfail.DNSRBLHandler;
import org.apache.james.smtpserver.junkscore.JunkScore;
import org.apache.james.smtpserver.junkscore.JunkScoreImpl;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.mailet.MailAddress;

import junit.framework.TestCase;

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
        mockedDnsServer = new AbstractDNSServer() {

            public Collection findMXRecords(String hostname) {
                throw new UnsupportedOperationException("Unimplemented in mock");
            }

            public Collection findTXTRecords(String hostname) {
                List res = new ArrayList();
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
        mockedSMTPSession = new AbstractSMTPSession() {
            HashMap state = new HashMap();
            HashMap connectionState = new HashMap();
            boolean stopHandler = false;
            
            public String getRemoteIPAddress() {
                return remoteIp;
            }

            public Map getState() {
            state.put(SMTPSession.CURRENT_RECIPIENT, rcpt);
                return state;
            }

            public boolean isRelayingAllowed() {
                return relaying;
            }

            public boolean isAuthRequired() {
                return false;
            }

            public int getRcptCount() {
                return 0;
            }

            public void setStopHandlerProcessing(boolean b) {
                stopHandler = b;  
            }

            public boolean getStopHandlerProcessing() {
                return stopHandler;
            }

            public Map getConnectionState() {       
                return connectionState;
            }

            public void resetConnectionState() {
                connectionState.clear();
            }

        };
    }

    // ip is blacklisted and has txt details
    public void testBlackListedTextPresent() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());

        setupMockedSMTPSession(new MailAddress("any@domain"));
        rbl.setDNSServer(mockedDnsServer);

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

        ContainerUtil.enableLogging(rbl, new MockLogger());

        setupMockedSMTPSession(new MailAddress("any@domain"));
        rbl.setDNSServer(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(false);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip is allowed to relay
    public void testRelayAllowed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());

        setRelayingAllowed(true);
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSServer(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip not on blacklist
    public void testNotBlackListed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());
        setRemoteIp("192.168.0.1");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSServer(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip on blacklist without txt details
    public void testBlackListedNoTxt() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());
        setRemoteIp("127.0.0.3");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSServer(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull(mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }

    // ip on whitelist
    public void testWhiteListed() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());
        setRemoteIp("127.0.0.2");
        setupMockedSMTPSession(new MailAddress("any@domain"));

        rbl.setDNSServer(mockedDnsServer);

        rbl.setWhitelist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(true);
        rbl.onConnect(mockedSMTPSession);
        assertNull(mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNull("Not blocked",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
    }
    
    public void testInvalidConfig() {
        boolean exception = false;
        DNSRBLHandler rbl = new DNSRBLHandler();
        try {
            rbl.configure((Configuration) new DefaultConfiguration("rblserver"));
        } catch (ConfigurationException e) {
            exception = true;
        }
        
        assertTrue("Invalid config",exception);
    }

    public void testAddJunkScore() throws ParseException {
        DNSRBLHandler rbl = new DNSRBLHandler();

        ContainerUtil.enableLogging(rbl, new MockLogger());

        setupMockedSMTPSession(new MailAddress("any@domain"));
        mockedSMTPSession.getConnectionState().put(JunkScore.JUNK_SCORE_SESSION, new JunkScoreImpl());
        rbl.setDNSServer(mockedDnsServer);

        rbl.setBlacklist(new String[] { "bl.spamcop.net." });
        rbl.setGetDetail(false);
        rbl.setScore(20);
        rbl.setAction("junkScore");
        rbl.onConnect(mockedSMTPSession);
        assertNull("No details",mockedSMTPSession.getConnectionState().get(RBL_DETAIL_MAIL_ATTRIBUTE_NAME));
        assertNotNull("Listed on RBL",mockedSMTPSession.getConnectionState().get(RBL_BLOCKLISTED_MAIL_ATTRIBUTE_NAME));
        
        rbl.onCommand(mockedSMTPSession);
        assertEquals("Score stored",((JunkScore) mockedSMTPSession.getConnectionState().get(JunkScore.JUNK_SCORE_SESSION)).getStoredScore("DNSRBLCheck"), 20.0, 0d);
    }

}
