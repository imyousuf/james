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
import org.apache.james.services.AbstractDNSServer;
import org.apache.james.services.DNSServer;
import org.apache.james.smtpserver.core.filter.fastfail.ResolvableEhloHeloHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.mailet.MailAddress;

public class ResolvableEhloHeloHandlerTest extends TestCase {

    public final static String INVALID_HOST = "foo.bar";
    
    public final static String VALID_HOST = "james.apache.org";
    
    public final static String HELO = "HELO";
    
    public final static String RCPT = "RCPT";
    
    
    private String command = null;
    
    public void setUp() {
        command = null;
    }
    
    private void setCommand(String command) {
        this.command = command;
    }
    
    private SMTPSession setupMockSession(final String argument,
             final boolean relaying, final boolean authRequired, final String user, final MailAddress recipient) {
        
        SMTPSession session = new AbstractSMTPSession() {

            HashMap connectionMap = new HashMap();
            HashMap map = new HashMap();
            SMTPResponse response = new SMTPResponse();
            
            public String getCommandArgument() {
                return argument;
            }
            
            public String getCommandName() {
                return command;
            }
            
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
                map.put(SMTPSession.CURRENT_RECIPIENT, recipient);
                return map;
            }
            
            public SMTPResponse getSMTPResponse() {
            return response;
            }
            
            
        };

        return session;
    }
    
    private DNSServer setupMockDNSServer() {
        DNSServer dns = new AbstractDNSServer(){
            public InetAddress getByName(String host) throws UnknownHostException {
                if (host.equals(INVALID_HOST)) 
                    throw new UnknownHostException();
                return null;
            }
        };
        
        return dns;
    }
    
    public void testRejectInvalidHelo() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,false,false,null,new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session, new Chain(null));
        assertNotNull("Invalid HELO",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session, new Chain(null));
        assertTrue("Reject", session.getSMTPResponse().retrieve().size() > 0);
    }
    
    
    public void testNotRejectValidHelo() throws ParseException {
        SMTPSession session = setupMockSession(VALID_HOST,false,false,null,new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session, new Chain(null));
        assertNull("Valid HELO",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session, new Chain(null));
        assertTrue("Not Reject", session.getSMTPResponse().retrieve().size() == 0);

    }
    
    public void testNotRejectInvalidHeloAuthUser() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,false,true,"valid@user",new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session, new Chain(null));
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session, new Chain(null));
        assertTrue("Not reject", session.getSMTPResponse().retrieve().size() == 0);
    }
    
    public void testRejectInvalidHeloAuthUser() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,false,true,"valid@user",new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        handler.setCheckAuthUsers(true);
        
        // helo
        setCommand(HELO);
        handler.onCommand(session, new Chain(null));
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session, new Chain(null));
        assertTrue("Reject", session.getSMTPResponse().retrieve().size() > 0);

    }
    
    public void testNotRejectRelay() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,true,false,null,new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session, new Chain(null));
        assertNull("Value not stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session, new Chain(null));
        assertTrue("Not reject", session.getSMTPResponse().retrieve().size() == 0);
        
    }
    
    public void testRejectRelay() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,true,false,null,new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        handler.setCheckAuthNetworks(true);
        
        // helo
        setCommand(HELO);
        handler.onCommand(session, new Chain(null));
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session, new Chain(null));
        assertTrue("Reject", session.getSMTPResponse().retrieve().size() > 0);

    }
    
    public void testNotRejectInvalidHeloPostmaster() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,false,false,null,new MailAddress("postmaster@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session, new Chain(null));
        assertNotNull("stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session, new Chain(null));
        assertTrue("Not reject", session.getSMTPResponse().retrieve().size() == 0);
        
    }
    
    public void testNotRejectInvalidHeloAbuse() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,false,false,null,new MailAddress("abuse@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session, new Chain(null));
        assertNotNull("stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session, new Chain(null));
        assertTrue("Not reject", session.getSMTPResponse().retrieve().size() == 0);

    }
}
