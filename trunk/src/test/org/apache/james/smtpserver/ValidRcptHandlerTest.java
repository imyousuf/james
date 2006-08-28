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

import javax.mail.internet.ParseException;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.smtpserver.core.filter.fastfail.AbstractVirtualUserTableHandler;
import org.apache.james.smtpserver.core.filter.fastfail.ValidRcptHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.mailet.MailAddress;
import org.apache.oro.text.regex.MalformedPatternException;

import junit.framework.TestCase;

public class ValidRcptHandlerTest extends TestCase {
    
    private final static String VALID_USER = "postmaster";
    private final static String INVALID_USER = "invalid";
    private String response = null;
    
    public void setUp() {
        response = null;
    }
    
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
        
            public void writeResponse(String resp) {
                response = resp;
            }
        
            public void setStopHandlerProcessing(boolean stop) {
                this.stop = stop;
            }
        
            public boolean getStopHandlerProcessing() {
                return stop;
            }
        };
    
        return session;
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
        
        };
    
        return conf;
    }
    
    public void testRejectInvalidUser() throws ParseException {
        ValidRcptHandler handler = new ValidRcptHandler();
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(INVALID_USER + "@localhost"),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.onCommand(session);
    
        assertTrue("Rejected",session.getStopHandlerProcessing());
        assertNotNull("Rejected",response);
    }
    
    public void testNotRejectInvalidUserAuth() throws ParseException {
        ValidRcptHandler handler = new ValidRcptHandler();
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(INVALID_USER + "@localhost"),false,true,"authedUser");
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.onCommand(session);
    
        assertFalse("Not rejected",session.getStopHandlerProcessing());
        assertNull("Not rejected",response);
    }
    
    public void testNotRejectInvalidUserRelay() throws ParseException {
        ValidRcptHandler handler = new ValidRcptHandler();
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(INVALID_USER + "@localhost"),true,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.onCommand(session);
    
        assertFalse("Not rejected",session.getStopHandlerProcessing());
        assertNull("Not rejected",response);
    }
    
    public void testNotRejectValidUser() throws ParseException {
        ValidRcptHandler handler = new ValidRcptHandler();
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(VALID_USER + "@localhost"),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.onCommand(session);
    
        assertFalse("Not rejected",session.getStopHandlerProcessing());
        assertNull("Not rejected",response);
    }
    
    public void testNotRejectValidUserRecipient() throws ParseException {
        String recipient = "recip@domain";
        ValidRcptHandler handler = new ValidRcptHandler();
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(recipient),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.setValidRecipients(recipient);
        handler.onCommand(session);
        
    
        assertFalse("Not rejected",session.getStopHandlerProcessing());
        assertNull("Not rejected",response);
    }
    
    public void testNotRejectValidUserDomain() throws ParseException {
        String domain = "domain";
        String recipient = "recip@" + domain;

        ValidRcptHandler handler = new ValidRcptHandler();
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(recipient),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.setValidDomains(domain);
        handler.onCommand(session);
        
    
        assertFalse("Not rejected",session.getStopHandlerProcessing());
        assertNull("Not rejected",response);
    }
    
    public void testNotRejectValidUserRegex() throws ParseException, MalformedPatternException {
        String domain = "domain";
        String recipient = "recip@" + domain;

        ValidRcptHandler handler = new ValidRcptHandler();
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(recipient),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.setValidRegex("reci.*");
        handler.onCommand(session);
        
    
        assertFalse("Not rejected",session.getStopHandlerProcessing());
        assertNull("Not rejected",response);
    }
    
    public void testInvalidRegex() throws ParseException{
        boolean exception = false;
        ValidRcptHandler handler = new ValidRcptHandler();
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        try {
            handler.setValidRegex("(.*");
        } catch (MalformedPatternException e) {
            exception = true;
        }

        assertTrue("Invalid Config",exception);
    }
    
    public void testNotRejectValidUserState() throws ParseException {
        String domain = "domain";
        String recipient = "recip@" + domain;
        ValidRcptHandler handler = new ValidRcptHandler();
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),new MailAddress(recipient),false,false,null);
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        session.getState().put(AbstractVirtualUserTableHandler.VALID_USER, recipient);
        handler.onCommand(session);
    
        assertFalse("Not rejected",session.getStopHandlerProcessing());
        assertNull("Not rejected",response);
    }
    
}
