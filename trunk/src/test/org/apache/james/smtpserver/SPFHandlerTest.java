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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.jspf.core.DNSService;
import org.apache.james.smtpserver.core.filter.fastfail.SPFHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.util.junkscore.JunkScore;
import org.apache.james.util.junkscore.JunkScoreImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import junit.framework.TestCase;

public class SPFHandlerTest extends TestCase {

    private DNSService mockedDnsService;

    private SMTPSession mockedSMTPSession;;

    private boolean relaying = false;

    private String command = "MAIL";

    protected void setUp() throws Exception {
        super.setUp();
        setupMockedDnsService();
        setRelayingAllowed(false);
    }

    /**
     * Set relayingAllowed
     * 
     * @param relaying
     *            true or false
     */
    private void setRelayingAllowed(boolean relaying) {
        this.relaying = relaying;
    }

    /**
     * Setup the mocked dnsserver
     * 
     */
    private void setupMockedDnsService() {
        mockedDnsService = new DNSService() {

            public List getLocalDomainNames() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setTimeOut(int arg0) {
                // do nothing
            }

            public int getRecordLimit() {
                return 0;
            }

            public void setRecordLimit(int arg0) {
                throw new UnsupportedOperationException(
                "Unimplemented mock service");
            }

            public List getRecords(String host, int type) throws TimeoutException {
                switch (type) {
                    case DNSService.TXT:
                    case DNSService.SPF:
                        List l = new ArrayList();
                        if (host.equals("spf1.james.apache.org")) {
                            // pass
                            l.add("v=spf1 +all");
                            return l;
                        } else if (host.equals("spf2.james.apache.org")) {
                            // fail
                            l.add("v=spf1 -all");
                            return l;
                        } else if (host.equals("spf3.james.apache.org")) {
                            // softfail
                            l.add("v=spf1 ~all");
                            return l;
                        } else if (host.equals("spf4.james.apache.org")) {
                            // permerror
                            l.add("v=spf1 badcontent!");
                            return l;
                        } else if (host.equals("spf5.james.apache.org")) {
                            // temperror
                            throw new TimeoutException();
                        } else {
                            return null;
                        }
                    default:
                        throw new UnsupportedOperationException(
                        "Unimplemented mock service");
                }
            }

        };
    }

    private void setCommand(String command) {
        this.command = command;
    }

    /**
     * Setup mocked smtpsession
     */
    private void setupMockedSMTPSession(final String ip, final String helo,
            final MailAddress sender, final MailAddress recipient) {
        mockedSMTPSession = new AbstractSMTPSession() {
            HashMap state = new HashMap();

            HashMap connectionState = new HashMap();

            Mail mail = new MockMail();

            boolean stopHandler = false;

            public void writeResponse(String respString) {
                // Do nothing
            }

            public String getCommandName() {
                return command;
            }

            public Mail getMail() {
                return mail;
            }

            public String getRemoteIPAddress() {
                return ip;
            }

            public Map getState() {
                state.put(SMTPSession.CURRENT_HELO_NAME, helo);
                state.put(SMTPSession.SENDER, sender);
                state.put(SMTPSession.CURRENT_RECIPIENT, recipient);
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

    private void runHandlers(SPFHandler spf, SMTPSession mockedSMTPSession) {

        setCommand("MAIL");
        spf.onCommand(mockedSMTPSession);

        setCommand("RCPT");
        spf.onCommand(mockedSMTPSession);

        spf.onMessage(mockedSMTPSession);
    }

    public void testSPFpass() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf1.james.apache.org",
                new MailAddress("test@spf1.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();


        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);
        
        spf.initialize();

        runHandlers(spf, mockedSMTPSession);

        assertNull("Not reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNull("Not blocked so no details", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_DETAIL));
        assertNull("No tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNotNull("Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertEquals("header", mockedSMTPSession.getState().get(
                SPFHandler.SPF_HEADER), mockedSMTPSession.getMail()
                .getAttribute(SPFHandler.SPF_HEADER_MAIL_ATTRIBUTE_NAME));
        assertFalse(mockedSMTPSession.getStopHandlerProcessing());
    }

    public void testSPFfail() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf2.james.apache.org",
                new MailAddress("test@spf2.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);     
        
        spf.initialize();

        runHandlers(spf, mockedSMTPSession);

        assertNotNull("reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNotNull("blocked", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNull("No tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNotNull("Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertTrue(mockedSMTPSession.getStopHandlerProcessing());
    }

    public void testSPFsoftFail() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf3.james.apache.org",
                new MailAddress("test@spf3.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);
        
        spf.initialize();

        runHandlers(spf, mockedSMTPSession);

        assertNull("not reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNull("no details ", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNull("No tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNotNull("Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertEquals("header", mockedSMTPSession.getState().get(
                SPFHandler.SPF_HEADER), mockedSMTPSession.getMail()
                .getAttribute(SPFHandler.SPF_HEADER_MAIL_ATTRIBUTE_NAME));
        assertFalse(mockedSMTPSession.getStopHandlerProcessing());
    }

    public void testSPFsoftFailRejectEnabled() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf3.james.apache.org",
                new MailAddress("test@spf3.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
       
        spf.setDNSService(mockedDnsService);
       
        spf.initialize();
        
        spf.setBlockSoftFail(true);

        setCommand("MAIL");
        spf.onCommand(mockedSMTPSession);

        setCommand("RCPT");
        spf.onCommand(mockedSMTPSession);

        assertNotNull("reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNotNull("details ", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNull("No tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNotNull("Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertTrue(mockedSMTPSession.getStopHandlerProcessing());
    }

    public void testSPFpermError() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf4.james.apache.org",
                new MailAddress("test@spf4.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);
        
        spf.initialize();
        
        spf.setBlockSoftFail(true);

        runHandlers(spf, mockedSMTPSession);

        assertNotNull("reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNotNull("details ", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNull("No tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNotNull("Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertTrue(mockedSMTPSession.getStopHandlerProcessing());
    }

    public void testSPFtempError() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf5.james.apache.org",
                new MailAddress("test@spf5.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);

        spf.initialize();
        
        runHandlers(spf, mockedSMTPSession);

        assertNull("no reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNull("no details ", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNotNull("tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNotNull("Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertTrue(mockedSMTPSession.getStopHandlerProcessing());
    }

    public void testSPFNoRecord() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf6.james.apache.org",
                new MailAddress("test@spf6.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

        spf.setDNSService(mockedDnsService);

        spf.initialize();
        
        runHandlers(spf, mockedSMTPSession);

        assertNull("no reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNull("no details ", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNull("no tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNotNull("Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertEquals("header", mockedSMTPSession.getState().get(
                SPFHandler.SPF_HEADER), mockedSMTPSession.getMail()
                .getAttribute(SPFHandler.SPF_HEADER_MAIL_ATTRIBUTE_NAME));
        assertFalse(mockedSMTPSession.getStopHandlerProcessing());
    }

    public void testSPFpermErrorNotRejectPostmaster() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf4.james.apache.org",
                new MailAddress("test@spf4.james.apache.org"), new MailAddress(
                        "postmaster@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

        spf.setDNSService(mockedDnsService);
        
        spf.initialize();
        
        spf.setBlockSoftFail(true);

        runHandlers(spf, mockedSMTPSession);

        assertNull("not reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNull("no details ", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNull("No tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNull("no Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertFalse(mockedSMTPSession.getStopHandlerProcessing());
    }

    public void testSPFpermErrorNotRejectAbuse() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf4.james.apache.org",
                new MailAddress("test@spf4.james.apache.org"), new MailAddress(
                        "abuse@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.initialize();

        spf.setDNSService(mockedDnsService);
        spf.setBlockSoftFail(true);

        runHandlers(spf, mockedSMTPSession);

        assertNull("not reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNull("no details ", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNull("No tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNull("no Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertFalse(mockedSMTPSession.getStopHandlerProcessing());
    }
    
    public void testSPFpermErrorRejectDisabled() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf4.james.apache.org",
                new MailAddress("test@spf4.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);
        
        spf.initialize();
        
        spf.setBlockPermError(false);

        runHandlers(spf, mockedSMTPSession);

        assertNull("not reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNull("details ", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNull("No tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNotNull("Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertFalse(mockedSMTPSession.getStopHandlerProcessing());
    }
    
    public void testSPFfailAddJunkScore() throws Exception {
        setupMockedSMTPSession("192.168.100.1", "spf2.james.apache.org",
                new MailAddress("test@spf2.james.apache.org"), new MailAddress(
                        "test@localhost"));
        mockedSMTPSession.getState().put(JunkScore.JUNK_SCORE, new JunkScoreImpl());
        
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        spf.setAction("junkScore");
        spf.setScore(20);
        spf.setDNSService(mockedDnsService);     
        
        spf.initialize();

        runHandlers(spf, mockedSMTPSession);

        assertNotNull("reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNotNull("blocked", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNull("No tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNotNull("Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertFalse("Not stopped", mockedSMTPSession.getStopHandlerProcessing());
        assertEquals("Score added",((JunkScore) mockedSMTPSession.getState().get(JunkScore.JUNK_SCORE)).getStoredScore("SPFCheck"), 20.0, 0d);
    }
}
