/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.                  *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.smtpserver;

import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;

import java.util.List;

import javax.mail.internet.ParseException;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.jspf.core.DNSService;
import org.apache.james.jspf.exceptions.NoneException;
import org.apache.james.jspf.exceptions.PermErrorException;
import org.apache.james.jspf.exceptions.TempErrorException;
import org.apache.james.smtpserver.core.filter.fastfail.SPFHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.util.watchdog.Watchdog;
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

            public List getAAAARecords(String arg0, int arg1)
                    throws NoneException, PermErrorException,
                    TempErrorException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public List getARecords(String arg0, int arg1)
                    throws NoneException, PermErrorException,
                    TempErrorException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public List getLocalDomainNames() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public List getMXRecords(String arg0, int arg1)
                    throws PermErrorException, NoneException,
                    TempErrorException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public List getPTRRecords(String arg0) throws PermErrorException,
                    NoneException, TempErrorException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getSpfRecord(String host, String version)
                    throws PermErrorException, NoneException,
                    TempErrorException {
                if (host.equals("spf1.james.apache.org")) {
                    // pass
                    return "v=spf1 +all";
                } else if (host.equals("spf2.james.apache.org")) {
                    // fail
                    return "v=spf1 -all";
                } else if (host.equals("spf3.james.apache.org")) {
                    // softfail
                    return "v=spf1 ~all";
                } else if (host.equals("spf4.james.apache.org")) {
                    // permerror
                    throw new PermErrorException("junit permerror test");
                } else if (host.equals("spf5.james.apache.org")) {
                    throw new TempErrorException("junit temperror test");
                } else {
                    throw new NoneException("junit noneerror test");
                }
            }

            public String getTxtCatType(String arg0) throws NoneException,
                    PermErrorException, TempErrorException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setTimeOut(int arg0) {
                // do nothing
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
        mockedSMTPSession = new SMTPSession() {
            HashMap state = new HashMap();

            HashMap connectionState = new HashMap();

            Mail mail = new MockMail();

            boolean stopHandler = false;

            public void writeResponse(String respString) {
                // Do nothing
            }

            public String readCommandLine() throws IOException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public StringBuffer getResponseBuffer() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String clearResponseBuffer() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public InputStream getInputStream() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getCommandName() {
                return command;
            }

            public String getCommandArgument() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Mail getMail() {
                return mail;
            }

            public void setMail(Mail mail) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteHost() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteIPAddress() {
                return ip;
            }

            public void abortMessage() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void endSession() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean isSessionEnded() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public HashMap getState() {
                state.put(SMTPSession.CURRENT_HELO_NAME, helo);
                state.put(SMTPSession.SENDER, sender);
                state.put(SMTPSession.CURRENT_RECIPIENT, recipient);
                return state;
            }

            public void resetState() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public SMTPHandlerConfigurationData getConfigurationData() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean isRelayingAllowed() {
                return relaying;
            }

            public boolean isAuthRequired() {
                return false;
            }

            public boolean useHeloEhloEnforcement() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getUser() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setUser(String user) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Watchdog getWatchdog() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getSessionID() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
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

            public HashMap getConnectionState() {
                return connectionState;
            }

            public void resetConnectionState() {
                connectionState.clear();
            }

            public void setRelayingAllowed(boolean relayingAllowed) {
                throw new UnsupportedOperationException(
                "Unimplemented mock service");
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

    public void testSPFpass() throws ParseException {
        setupMockedSMTPSession("192.168.100.1", "spf1.james.apache.org",
                new MailAddress("test@spf1.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

        spf.setDNSService(mockedDnsService);

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

    public void testSPFfail() throws ParseException {
        setupMockedSMTPSession("192.168.100.1", "spf2.james.apache.org",
                new MailAddress("test@spf2.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

        spf.setDNSService(mockedDnsService);

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

    public void testSPFsoftFail() throws ParseException {
        setupMockedSMTPSession("192.168.100.1", "spf3.james.apache.org",
                new MailAddress("test@spf3.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

        spf.setDNSService(mockedDnsService);

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

    public void testSPFsoftFailRejectEnabled() throws ParseException {
        setupMockedSMTPSession("192.168.100.1", "spf3.james.apache.org",
                new MailAddress("test@spf3.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

        spf.setDNSService(mockedDnsService);
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

    public void testSPFpermError() throws ParseException {
        setupMockedSMTPSession("192.168.100.1", "spf4.james.apache.org",
                new MailAddress("test@spf4.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

        spf.setDNSService(mockedDnsService);
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

    public void testSPFtempError() throws ParseException {
        setupMockedSMTPSession("192.168.100.1", "spf5.james.apache.org",
                new MailAddress("test@spf5.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

        spf.setDNSService(mockedDnsService);

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

    public void testSPFNoRecord() throws ParseException {
        setupMockedSMTPSession("192.168.100.1", "spf6.james.apache.org",
                new MailAddress("test@spf6.james.apache.org"), new MailAddress(
                        "test@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

        spf.setDNSService(mockedDnsService);

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

    public void testSPFpermErrorNotRejectPostmaster() throws ParseException {
        setupMockedSMTPSession("192.168.100.1", "spf4.james.apache.org",
                new MailAddress("test@spf4.james.apache.org"), new MailAddress(
                        "postmaster@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

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

    public void testSPFpermErrorNotRejectAbuse() throws ParseException {
        setupMockedSMTPSession("192.168.100.1", "spf4.james.apache.org",
                new MailAddress("test@spf4.james.apache.org"), new MailAddress(
                        "abuse@localhost"));
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

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
}
