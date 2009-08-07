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
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.smtpserver.core.filter.fastfail.ResolvableEhloHeloHandler;
import org.apache.james.smtpserver.junkscore.JunkScore;
import org.apache.james.smtpserver.junkscore.JunkScoreImpl;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.mailet.MailAddress;

public class ResolvableEhloHeloHandlerTest extends TestCase {

    public final static String INVALID_HOST = "foo.bar";
    
    public final static String VALID_HOST = "james.apache.org";
    
    public final static String HELO = "HELO";
    
    public final static String RCPT = "RCPT";
    
    
    private SMTPSession setupMockSession(final boolean relaying, final boolean authRequired, final String user) {
        
        SMTPSession session = new AbstractSMTPSession() {

            HashMap connectionMap = new HashMap();
            HashMap map = new HashMap();
            
            
            public boolean isAuthRequired() {
                return authRequired;
            }
            
            public String getUser() {
                return user;
            }
            
            public Map getConnectionState() {
                return connectionMap;
            }
            
            
            public boolean isRelayingAllowed() {
                return relaying;
            }

            public Map getState() {
                return map;
            }
            
            
        };

        return session;
    }
    
    private DNSService setupMockDNSServer() {
        DNSService dns = new AbstractDNSServer(){
            public InetAddress getByName(String host) throws UnknownHostException {
                if (host.equals(INVALID_HOST)) 
                    throw new UnknownHostException();
                return InetAddress.getLocalHost();
            }
        };
        
        return dns;
    }
    
    public void testRejectInvalidHelo() throws ParseException {
        SMTPSession session = setupMockSession(false,false,null);
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        assertNull(handler.onHelo(session,INVALID_HOST));
        assertNotNull("Invalid HELO",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        assertNotNull("Reject",handler.onRcpt(session,new MailAddress("test@localhost")));
        
    }
    
    
    public void testNotRejectValidHelo() throws ParseException {
        SMTPSession session = setupMockSession(false,false,null);
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        assertNull(handler.onHelo(session,VALID_HOST));
        assertNull("Valid HELO",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        assertNull("Not reject", handler.onRcpt(session,new MailAddress("test@localhost")));
    }
    
    public void testNotRejectInvalidHeloAuthUser() throws ParseException {
        SMTPSession session = setupMockSession(false,true,"valid@user");
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        handler.onHelo(session,INVALID_HOST);
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        assertNull("Not reject", handler.onRcpt(session,new MailAddress("test@localhost")));
    }
    
    public void testRejectInvalidHeloAuthUser() throws ParseException {
        SMTPSession session = setupMockSession(false,true,"valid@user");
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        handler.setCheckAuthUsers(true);
        
        // helo
        handler.onHelo(session,INVALID_HOST);
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        assertNotNull("reject", handler.onRcpt(session,new MailAddress("test@localhost")));
       
    }
    
    public void testNotRejectRelay() throws ParseException {
        SMTPSession session = setupMockSession(true,false,null);
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        assertNull(handler.onHelo(session,INVALID_HOST));
        assertNull("Value not stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        assertNull("Not reject", handler.onRcpt(session,new MailAddress("test@localhost")));
        
    }
    
    public void testRejectRelay() throws ParseException {
        SMTPSession session = setupMockSession(true,false,null);
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        handler.setCheckAuthNetworks(true);
        
        // helo
        assertNull(handler.onHelo(session,INVALID_HOST));
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        assertNotNull("Reject", handler.onRcpt(session,new MailAddress("test@localhost")));
        
    }
    
    public void testNotRejectInvalidHeloPostmaster() throws ParseException {
        SMTPSession session = setupMockSession(false,false,null);
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        assertNull(handler.onHelo(session,INVALID_HOST));
        assertNotNull("stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        assertNull("Not Reject", handler.onRcpt(session,new MailAddress("postmaster@localhost")));
    }
    
    public void testNotRejectInvalidHeloAbuse() throws ParseException {
        SMTPSession session = setupMockSession(false,false,null);
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        assertNull(handler.onHelo(session,INVALID_HOST));
        assertNotNull("stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        assertNull("Not Reject", handler.onRcpt(session,new MailAddress("abuse@localhost")));
    }
    
}
