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
import org.apache.james.jspf.core.DNSRequest;
import org.apache.james.jspf.core.DNSService;
import org.apache.james.jspf.core.exceptions.TimeoutException;
import org.apache.james.smtpserver.core.filter.fastfail.SPFHandler;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.mailet.base.test.FakeMail;
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

            public List getRecords(DNSRequest req) throws TimeoutException {
                switch (req.getRecordType()) {
                    case DNSRequest.TXT:
                    case DNSRequest.SPF:
                        List l = new ArrayList();
                        if (req.getHostname().equals("spf1.james.apache.org")) {
                            // pass
                            l.add("v=spf1 +all");
                            return l;
                        } else if (req.getHostname().equals("spf2.james.apache.org")) {
                            // fail
                            l.add("v=spf1 -all");
                            return l;
                        } else if (req.getHostname().equals("spf3.james.apache.org")) {
                            // softfail
                            l.add("v=spf1 ~all");
                            return l;
                        } else if (req.getHostname().equals("spf4.james.apache.org")) {
                            // permerror
                            l.add("v=spf1 badcontent!");
                            return l;
                        } else if (req.getHostname().equals("spf5.james.apache.org")) {
                            // temperror
                            throw new TimeoutException("TIMEOUT");
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
    private void setupMockedSMTPSession(final String ip, final String helo) {
        mockedSMTPSession = new BaseFakeSMTPSession() {
            HashMap state = new HashMap();

            HashMap connectionState = new HashMap();

            Mail mail = new FakeMail();

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

            public Map getConnectionState() {
                return connectionState;
            }

            public void resetConnectionState() {
                connectionState.clear();
            }

        };
    }

    public void testSPFpass() throws Exception {
    	MailAddress sender = new MailAddress("test@spf1.james.apache.org");
    	MailAddress rcpt = new MailAddress("test@localhost");
        setupMockedSMTPSession("192.168.100.1", "spf1.james.apache.org");
        SPFHandler spf = new SPFHandler();


        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);
        
        assertEquals("declined",HookReturnCode.DECLINED, spf.doMail(mockedSMTPSession, sender).getResult());
        assertEquals("declined", HookReturnCode.DECLINED, spf.doRcpt(mockedSMTPSession, sender, rcpt).getResult());
    }

    public void testSPFfail() throws Exception {
    	MailAddress sender = new MailAddress("test@spf2.james.apache.org");
    	MailAddress rcpt = new MailAddress("test@localhost");
        setupMockedSMTPSession("192.168.100.1", "spf2.james.apache.org");
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);     

        assertEquals("declined",HookReturnCode.DECLINED, spf.doMail(mockedSMTPSession, sender).getResult());
        assertEquals("fail", HookReturnCode.DENY, spf.doRcpt(mockedSMTPSession, sender, rcpt).getResult());
    }

    public void testSPFsoftFail() throws Exception {
    	MailAddress sender = new MailAddress("test@spf3.james.apache.org");
    	MailAddress rcpt = new MailAddress("test@localhost");
        setupMockedSMTPSession("192.168.100.1", "spf3.james.apache.org");
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);
        
        assertEquals("declined",HookReturnCode.DECLINED, spf.doMail(mockedSMTPSession, sender).getResult());
        assertEquals("softfail declined", HookReturnCode.DECLINED, spf.doRcpt(mockedSMTPSession, sender, rcpt).getResult());
    }

    public void testSPFsoftFailRejectEnabled() throws Exception {
    	MailAddress sender = new MailAddress("test@spf3.james.apache.org");
    	MailAddress rcpt = new MailAddress("test@localhost");
    	
        setupMockedSMTPSession("192.168.100.1", "spf3.james.apache.org");
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
       
        spf.setDNSService(mockedDnsService);
        
        spf.setBlockSoftFail(true);

        assertEquals("declined",HookReturnCode.DECLINED, spf.doMail(mockedSMTPSession, sender).getResult());
        assertEquals("softfail reject", HookReturnCode.DENY, spf.doRcpt(mockedSMTPSession, sender, rcpt).getResult());
    }

    public void testSPFpermError() throws Exception {
    	MailAddress sender = new MailAddress("test@spf4.james.apache.org");
    	MailAddress rcpt = new MailAddress("test@localhost");
    	
        setupMockedSMTPSession("192.168.100.1", "spf4.james.apache.org");
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);
        
        spf.setBlockSoftFail(true);

        assertEquals("declined",HookReturnCode.DECLINED, spf.doMail(mockedSMTPSession, sender).getResult());
        assertEquals("permerror reject", HookReturnCode.DENY, spf.doRcpt(mockedSMTPSession, sender, rcpt).getResult());
    }

    public void testSPFtempError() throws Exception {
    	MailAddress sender = new MailAddress("test@spf5.james.apache.org");
    	MailAddress rcpt = new MailAddress("test@localhost");
    	
        setupMockedSMTPSession("192.168.100.1", "spf5.james.apache.org");
        
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);

        assertEquals("declined",HookReturnCode.DECLINED, spf.doMail(mockedSMTPSession, sender).getResult());
        assertEquals("temperror denysoft", HookReturnCode.DENYSOFT, spf.doRcpt(mockedSMTPSession, sender, rcpt).getResult());
    }

    public void testSPFNoRecord() throws Exception {
    	MailAddress sender = new MailAddress("test@spf6.james.apache.org");
    	MailAddress rcpt = new MailAddress("test@localhost");
    	
        setupMockedSMTPSession("192.168.100.1", "spf6.james.apache.org");
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());

        spf.setDNSService(mockedDnsService);

        assertEquals("declined",HookReturnCode.DECLINED, spf.doMail(mockedSMTPSession, sender).getResult());
        assertEquals("declined", HookReturnCode.DECLINED, spf.doRcpt(mockedSMTPSession, sender, rcpt).getResult());
    }

    
    public void testSPFpermErrorRejectDisabled() throws Exception {
    	MailAddress sender = new MailAddress("test@spf4.james.apache.org");
    	MailAddress rcpt = new MailAddress("test@localhost");
        setupMockedSMTPSession("192.168.100.1", "spf4.james.apache.org");
        SPFHandler spf = new SPFHandler();

        ContainerUtil.enableLogging(spf, new MockLogger());
        
        spf.setDNSService(mockedDnsService);
        
        spf.setBlockPermError(false);

        assertEquals("declined",HookReturnCode.DECLINED, spf.doMail(mockedSMTPSession, sender).getResult());
        assertEquals("declined", HookReturnCode.DECLINED, spf.doRcpt(mockedSMTPSession, sender, rcpt).getResult());
    }
    
   
}
