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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.james.protocols.smtp.BaseFakeSMTPSession;
import org.apache.james.protocols.smtp.SMTPConfiguration;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.hook.HookReturnCode;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.integration.fastfail.ValidRcptHandler;
import org.apache.james.test.mock.james.MockVirtualUserTableStore;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

public class ValidRcptHandlerTest extends TestCase {
    
    private final static String VALID_DOMAIN = "localhost";
    private final static String VALID_USER = "postmaster";
    private final static String INVALID_USER = "invalid";
    private final static String USER1 = "user1";
    private final static String USER2 = "user2";
    
    UsersRepository users;
    ValidRcptHandler handler;
    
    @Override
    protected void setUp() throws Exception {
        users = new MockUsersRepository();
        users.addUser(VALID_USER,"xxx");
        handler = new ValidRcptHandler();
        handler.setUsers(users);
        handler.setTableStore(setUpVirtualUserTableStore());
        handler.setMailServer(new MailServer() {

            public boolean addUser(String userName, String password) {
                // TODO Auto-generated method stub
                return false;
            }

            public String getDefaultDomain() {
                // TODO Auto-generated method stub
                return null;
            }

            public String getHelloName() {
                // TODO Auto-generated method stub
                return null;
            }

            public String getId() {
                // TODO Auto-generated method stub
                return null;
            }

            public MailRepository getUserInbox(String userName) {
                // TODO Auto-generated method stub
                return null;
            }

            public boolean isLocalServer(String serverName) {
                return serverName.equals(VALID_DOMAIN);
            }

            public void sendMail(MailAddress sender, Collection<MailAddress> recipients, MimeMessage msg) throws MessagingException {
                // TODO Auto-generated method stub
                
            }

            public void sendMail(MailAddress sender, Collection<MailAddress> recipients, InputStream msg) throws MessagingException {
                // TODO Auto-generated method stub
                
            }

            public void sendMail(Mail mail) throws MessagingException {
                // TODO Auto-generated method stub
                
            }

            public void sendMail(MimeMessage message) throws MessagingException {
                // TODO Auto-generated method stub
                
            }

            public boolean supportVirtualHosting() {
                // TODO Auto-generated method stub
                return false;
            }
            
        });
        handler.init();
    }

    private SMTPSession setupMockedSMTPSession(final SMTPConfiguration conf, final MailAddress rcpt, final boolean relayingAllowed) {
        SMTPSession session = new BaseFakeSMTPSession() {
            HashMap<String,Object> state = new HashMap<String,Object>();

            public boolean isRelayingAllowed() {
                return relayingAllowed;
            }
        
            public Map<String,Object> getState() {
                return state;
            }
        };
    
        return session;
    }
    
    private VirtualUserTableStore setUpVirtualUserTableStore() {
        final VirtualUserTable table = new VirtualUserTable() {
 
            public Collection<String> getMappings(String user, String domain) throws ErrorMappingException {
                Collection<String> mappings = new ArrayList<String>();
                if (user.equals(USER1)) {
                    mappings.add("address@localhost");
                } else if (user.equals(USER2)) {
                    throw new ErrorMappingException("554 BOUNCE");
                }
                return mappings;
            }
        };
        final MockVirtualUserTableStore store = new MockVirtualUserTableStore() {

            @Override
            public VirtualUserTable getTable(String name) {
                return table;
            }
            
        };
        return store;
    }
    
    private SMTPConfiguration setupMockedSMTPConfiguration() {
        SMTPConfiguration conf = new SMTPConfiguration() {
            
        
            public String getHelloName() {
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


            public boolean isRelayingAllowed(String remoteIP) {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public boolean useHeloEhloEnforcement() {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
            }

            public boolean useAddressBracketsEnforcement() {
                return  true;
            }

			public boolean isAuthRequired(String remoteIP) {
                throw new UnsupportedOperationException("Unimplemented Stub Method");
			}

			public boolean isStartTLSSupported() {
				return false;
			}
        };
    
        return conf;
    }
    
    public void testRejectInvalidUser() throws Exception {
        MailAddress mailAddress = new MailAddress(INVALID_USER + "@localhost");
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),mailAddress,false);
    
        int rCode = handler.doRcpt(session, null, mailAddress).getResult();
    
        assertEquals("Rejected",rCode,HookReturnCode.DENY);
    }
    
    public void testNotRejectInvalidUserRelay() throws Exception {
        MailAddress mailAddress = new MailAddress(INVALID_USER + "@localhost");
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),mailAddress,true);

        int rCode = handler.doRcpt(session, null, mailAddress).getResult();
        
        assertEquals("Not rejected",rCode,HookReturnCode.DECLINED);
    }
    
    public void testNotRejectValidUser() throws Exception {
        MailAddress mailAddress = new MailAddress(VALID_USER + "@localhost");
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),mailAddress,false);
    
        int rCode = handler.doRcpt(session, null, mailAddress).getResult();
        
        assertEquals("Not rejected",rCode,HookReturnCode.DECLINED);
    }
  
    public void testHasAddressMapping() throws Exception {
        MailAddress mailAddress = new MailAddress(USER1 + "@localhost");
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),mailAddress,false);
    
        int rCode = handler.doRcpt(session, null, mailAddress).getResult();
        
        assertEquals("Not rejected",rCode,HookReturnCode.DECLINED);
    }
    
    public void testHasErrorMapping() throws Exception {
        MailAddress mailAddress = new MailAddress(USER2 + "@localhost");
        SMTPSession session = setupMockedSMTPSession(setupMockedSMTPConfiguration(),mailAddress,false);

        int rCode = handler.doRcpt(session, null,mailAddress).getResult();
    
        assertNull("Valid Error mapping",session.getState().get("VALID_USER"));
        assertEquals("Error mapping",rCode, HookReturnCode.DENY);
    }
}
