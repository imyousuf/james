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
import org.apache.james.api.dnsserver.DNSServer;
import org.apache.james.services.AbstractDNSServer;
import org.apache.james.smtpserver.core.filter.fastfail.ResolvableEhloHeloHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.util.junkscore.JunkScore;
import org.apache.james.util.junkscore.JunkScoreImpl;
import org.apache.mailet.MailAddress;

public class ResolvableEhloHeloHandlerTest extends TestCase {

    public final static String INVALID_HOST = "foo.bar";
    
    public final static String VALID_HOST = "james.apache.org";
    
    public final static String HELO = "HELO";
    
    public final static String RCPT = "RCPT";
    
    private String response = null;
    
    private String command = null;
    
    public void setUp() {
        response = null;
        command = null;
    }
    
    private String getResponse() {
        return response;
    }
    
    private void setCommand(String command) {
        this.command = command;
    }
    
    private SMTPSession setupMockSession(final String argument,
             final boolean relaying, final boolean authRequired, final String user, final MailAddress recipient) {
        
        SMTPSession session = new AbstractSMTPSession() {

            boolean stop = false;
            HashMap connectionMap = new HashMap();
            HashMap map = new HashMap();
            
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
            
            public void writeResponse(String resp) {
                response = resp;
            }
            
            public boolean isRelayingAllowed() {
                return relaying;
            }
            
            public void setStopHandlerProcessing(boolean stop) {
                this.stop = stop;
            }
            
            public boolean getStopHandlerProcessing() {
                return stop;
            }
            
            public Map getState() {
                map.put(SMTPSession.CURRENT_RECIPIENT, recipient);
                return map;
            }
            
            
        };

        return session;
    }
    
    private DNSServer setupMockDNSServer() {
        DNSServer dns = new AbstractDNSServer(){
            public InetAddress getByName(String host) throws UnknownHostException {
                if (host.equals(INVALID_HOST)) 
                    throw new UnknownHostException();
                return InetAddress.getLocalHost();
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
        handler.onCommand(session);
        assertNotNull("Invalid HELO",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session);
        assertNotNull("Reject", getResponse());
        
        assertTrue("Stop handler processing",session.getStopHandlerProcessing());
    }
    
    
    public void testNotRejectValidHelo() throws ParseException {
        SMTPSession session = setupMockSession(VALID_HOST,false,false,null,new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session);
        assertNull("Valid HELO",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session);
        assertNull("Not reject", getResponse());
        
        assertFalse("Not stop handler processing",session.getStopHandlerProcessing());
    }
    
    public void testNotRejectInvalidHeloAuthUser() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,false,true,"valid@user",new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session);
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session);
        assertNull("Not reject", getResponse());
        
        assertFalse("Not stop handler processing",session.getStopHandlerProcessing());
    }
    
    public void testRejectInvalidHeloAuthUser() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,false,true,"valid@user",new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        handler.setCheckAuthUsers(true);
        
        // helo
        setCommand(HELO);
        handler.onCommand(session);
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session);
        assertNotNull("reject", getResponse());
        
        assertTrue("stop handler processing",session.getStopHandlerProcessing());
    }
    
    public void testNotRejectRelay() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,true,false,null,new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session);
        assertNull("Value not stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session);
        assertNull("Not reject", getResponse());
        
        assertFalse("Not stop handler processing",session.getStopHandlerProcessing());
    }
    
    public void testRejectRelay() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,true,false,null,new MailAddress("test@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        handler.setCheckAuthNetworks(true);
        
        // helo
        setCommand(HELO);
        handler.onCommand(session);
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session);
        assertNotNull("Reject", getResponse());
        
        assertTrue("Stop handler processing",session.getStopHandlerProcessing());
    }
    
    public void testNotRejectInvalidHeloPostmaster() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,false,false,null,new MailAddress("postmaster@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session);
        assertNotNull("stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session);
        assertNull("Not Reject", getResponse());
        
        assertFalse("Not stop handler processing",session.getStopHandlerProcessing());
    }
    
    public void testNotRejectInvalidHeloAbuse() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,false,false,null,new MailAddress("abuse@localhost"));
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        
        // helo
        setCommand(HELO);
        handler.onCommand(session);
        assertNotNull("stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session);
        assertNull("Not Reject", getResponse());
        
        assertFalse("Not stop handler processing",session.getStopHandlerProcessing());
    }
    
    public void testAddJunkScoreInvalidHelo() throws ParseException {
        SMTPSession session = setupMockSession(INVALID_HOST,false,false,null,new MailAddress("test@localhost"));
        session.getConnectionState().put(JunkScore.JUNK_SCORE_SESSION, new JunkScoreImpl());
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setDnsServer(setupMockDNSServer());
        handler.setAction("junkScore");
        handler.setScore(20);
        
        // helo
        setCommand(HELO);
        handler.onCommand(session);
        assertNotNull("Invalid HELO",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        // rcpt
        setCommand(RCPT);
        handler.onCommand(session);
        assertNull("Not Reject", getResponse());
        
        assertFalse("Don'T stop handler processing",session.getStopHandlerProcessing());
        assertEquals("JunkScore added", ((JunkScore) session.getConnectionState().get(JunkScore.JUNK_SCORE_SESSION)).getStoredScore("ResolvableEhloHeloCheck"), 20.0, 0d);
    }
}
