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

import java.util.HashMap;
import java.util.Map;

import java.util.List;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.jspf.core.DNSService;
import org.apache.james.jspf.exceptions.NoneException;
import org.apache.james.jspf.exceptions.PermErrorException;
import org.apache.james.jspf.exceptions.TempErrorException;
import org.apache.james.smtpserver.core.filter.fastfail.SPFHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import junit.framework.TestCase;

public class SPFHandlerTest extends TestCase {

    private DNSService mockedDnsService;

    private SMTPSession mockedSMTPSession;;

    private boolean relaying = false;

    private String command = "MAIL";
    
    private final static int REJECT_CODE = 530;
    
    private final static int TEMP_REJECT_CODE = 451;

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
        mockedSMTPSession = new AbstractSMTPSession() {
            HashMap state = new HashMap();

            HashMap connectionState = new HashMap();

            Mail mail = new MockMail();
            
            SMTPResponse response = new SMTPResponse(500,"Unknown Response");

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

            public Map getConnectionState() {
                return connectionState;
            }

            public void resetConnectionState() {
                connectionState.clear();
            }
            
            public SMTPResponse getSMTPResponse() {
                return response;
            }

        };
    }

    private void runHandlers(SPFHandler spf, SMTPSession mockedSMTPSession) {

        setCommand("MAIL");
        spf.onCommand(mockedSMTPSession,new Chain(null));

        setCommand("RCPT");
        spf.onCommand(mockedSMTPSession,new Chain(null));

        spf.onMessage(mockedSMTPSession,new Chain(null));
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
        assertTrue("Not rejected", mockedSMTPSession.getSMTPResponse().getSMTPCode() != REJECT_CODE);
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
        assertTrue("Rejected", mockedSMTPSession.getSMTPResponse().getSMTPCode() == REJECT_CODE);
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
        assertTrue("Not rejected", mockedSMTPSession.getSMTPResponse().getSMTPCode() != REJECT_CODE);
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

        runHandlers(spf, mockedSMTPSession);

        assertNotNull("reject", mockedSMTPSession.getState().get(
                SPFHandler.SPF_BLOCKLISTED));
        assertNotNull("details ", mockedSMTPSession.getState().get(
                SPFHandler.SPF_DETAIL));
        assertNull("No tempError", mockedSMTPSession.getState().get(
                SPFHandler.SPF_TEMPBLOCKLISTED));
        assertNotNull("Header should present", mockedSMTPSession.getState()
                .get(SPFHandler.SPF_HEADER));
        assertTrue("Rejected", mockedSMTPSession.getSMTPResponse().getSMTPCode() == REJECT_CODE);
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
        assertTrue("Rejected", mockedSMTPSession.getSMTPResponse().getSMTPCode() == REJECT_CODE);
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
        assertTrue("Rejected", mockedSMTPSession.getSMTPResponse().getSMTPCode() == TEMP_REJECT_CODE);;
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
        assertTrue("Not rejected", mockedSMTPSession.getSMTPResponse().getSMTPCode() != REJECT_CODE);
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
        assertTrue("Not rejected", mockedSMTPSession.getSMTPResponse().getSMTPCode() != REJECT_CODE);
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
        assertTrue("Not rejected", mockedSMTPSession.getSMTPResponse().getSMTPCode() != REJECT_CODE);
    }
}
