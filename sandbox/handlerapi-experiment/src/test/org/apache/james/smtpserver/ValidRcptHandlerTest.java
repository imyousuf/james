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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.VirtualUserTable;
import org.apache.james.smtpserver.core.filter.fastfail.ValidRcptHandler;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.james.vut.ErrorMappingException;
import org.apache.mailet.MailAddress;
import org.apache.oro.text.regex.MalformedPatternException;

import junit.framework.TestCase;

public class ValidRcptHandlerTest extends TestCase {
    
    private final static String VALID_USER = "postmaster";
    private final static String INVALID_USER = "invalid";
    private final static String USER1 = "user1";
    private final static String USER2 = "user2";
    private MockServiceManager serviceMan;
    
    private SMTPSession setupMockedSMTPSession(final SMTPHandlerConfigurationData conf, final MailAddress rcpt, final boolean relayingAllowed, final boolean authRequired, final String username) {
        SMTPSession session = new AbstractSMTPSession() {
            HashMap state = new HashMap();
            boolean stop = false;
        
            public boolean isAuthRequired() {
                return authRequired;
            }
        
            public String getUser() {
                return username;
            }
        
            public boolean isRelayingAllowed() {
                return relayingAllowed;
            }
        
            public SMTPHandlerConfigurationData getConfigurationData() {
                return conf;
            }
        
            public Map getState() {
                state.put(SMTPSession.CURRENT_RECIPIENT,rcpt);

                return state;
            }
        };
    
        return session;
    }
    
    private MockServiceManager setUpServiceManager() throws Exception {
        serviceMan = new MockServiceManager();
        serviceMan.put(VirtualUserTable.ROLE, setUpVirtualUserTable());
        return serviceMan;
    }
    
    private VirtualUserTable setUpVirtualUserTable() {
        VirtualUserTable table = new VirtualUserTable() {
 
            public Collection getMappings(String user, String domain) throws ErrorMappingException {
                Collection mappings = new ArrayList();
                if (user.equals(USER1)) {
                    mappings.add("address@localhost");
                } else if (user.equals(USER2)) {
                    throw new ErrorMappingException("554 BOUNCE");
                }
                return mappings;
            }
        };
        return table;
    }
    
    private SMTPHandlerConfigurationData setupMockedSMTPConfiguration() {
        SMTPHandlerConfigurationData conf = new SMTPHandlerConfigurationData() {
            UsersRepository user = new MockUsersRepository();
        
            public String getHelloName() {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public MailServer getMailServer() {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public long getMaxMessageSize() {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public int getResetLength() {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public String getSMTPGreeting() {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public UsersRepository getUsersRepository() {
                user.addUser(VALID_USER,"xxx");
                return user;
            }

            public boolean isAuthRequired(String remoteIP) {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public boolean isAuthRequired() {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public boolean isRelayingAllowed(String remoteIP) {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public boolean isVerifyIdentity() {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public boolean useHeloEhloEnforcement() {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public boolean useAddressBracketsEnforcement() {
                return  true;
            }
        
        };
    
        return conf;
    }
    
    public void testRejectInvalidUser() throws Exception {
        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.service(handler, setUpServiceManager());
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(INVALID_USER + "@localhost"),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        int rCode = handler.doRcpt(session, null, (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT)).getResult();
    
        assertEquals("Rejected",rCode,HookReturnCode.DENY);
    }
    
    public void testNotRejectInvalidUserAuth() throws Exception {
        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.service(handler, setUpServiceManager());
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(INVALID_USER + "@localhost"),false,true,"authedUser");
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        int rCode = handler.doRcpt(session, null, (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT)).getResult();
        
        assertEquals("Not rejected",rCode,HookReturnCode.DECLINED);
    }
    
    public void testNotRejectInvalidUserRelay() throws Exception {
        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.service(handler, setUpServiceManager());
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(INVALID_USER + "@localhost"),true,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());

        int rCode = handler.doRcpt(session, null, (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT)).getResult();
        
        assertEquals("Not rejected",rCode,HookReturnCode.DECLINED);
    }
    
    public void testNotRejectValidUser() throws Exception {
        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.service(handler, setUpServiceManager());
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(VALID_USER + "@localhost"),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        int rCode = handler.doRcpt(session, null, (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT)).getResult();
        
        assertEquals("Not rejected",rCode,HookReturnCode.DECLINED);
    }
    
    public void testNotRejectValidUserRecipient() throws Exception {
        String recipient = "recip@domain";
        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.service(handler, setUpServiceManager());
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(recipient),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.setValidRecipients(recipient);

        int rCode = handler.doRcpt(session, null, (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT)).getResult();
        
        assertEquals("Not rejected",rCode,HookReturnCode.DECLINED);
    }
    
    public void testNotRejectValidUserDomain() throws Exception {
        String domain = "domain";
        String recipient = "recip@" + domain;

        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.service(handler, setUpServiceManager());
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(recipient),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.setValidDomains(domain);

        int rCode = handler.doRcpt(session, null, (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT)).getResult();
        
        assertEquals("Not rejected",rCode,HookReturnCode.DECLINED);
    }
    
    public void testNotRejectValidUserRegex() throws Exception {
        String domain = "domain";
        String recipient = "recip@" + domain;

        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.service(handler, setUpServiceManager());
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(recipient),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.setValidRegex("reci.*");

        int rCode = handler.doRcpt(session, null, (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT)).getResult();
        
        assertEquals("Not rejected",rCode,HookReturnCode.DECLINED);
    }
    
    public void testInvalidRegex() throws Exception{
        boolean exception = false;
        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.service(handler, setUpServiceManager());
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        try {
            handler.setValidRegex("(.*");
        } catch (MalformedPatternException e) {
            exception = true;
        }

        assertTrue("Invalid Config",exception);
    }
    
    public void testHasAddressMapping() throws Exception {
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(USER1 + "@localhost"),false,false,null);
    
        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.service(handler, setUpServiceManager());
        ContainerUtil.enableLogging(handler,new MockLogger());

        int rCode = handler.doRcpt(session, null, (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT)).getResult();
        
        assertEquals("Not rejected",rCode,HookReturnCode.DECLINED);
    }
    
    public void testHasErrorMapping() throws Exception {
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(USER2 + "@localhost"),false,false,null);

        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.service(handler, setUpServiceManager());
        ContainerUtil.enableLogging(handler,new MockLogger());

        int rCode = handler.doRcpt(session, null,(MailAddress)session.getState().get(SMTPSession.CURRENT_RECIPIENT)).getResult();
    
        assertNull("Valid Error mapping",session.getState().get("VALID_USER"));
        assertEquals("Error mapping",rCode, HookReturnCode.DENY);
    }
}
