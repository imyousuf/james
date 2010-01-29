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



package org.apache.james.smtpserver.protocol.core.fastfail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.james.smtpserver.protocol.BaseFakeDNSService;
import org.apache.james.smtpserver.protocol.BaseFakeSMTPSession;
import org.apache.james.smtpserver.protocol.DNSService;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.core.fastfail.ResolvableEhloHeloHandler;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.mailet.MailAddress;

public class ResolvableEhloHeloHandlerTest extends TestCase {

    public final static String INVALID_HOST = "foo.bar";
    
    public final static String VALID_HOST = "james.apache.org";


    private SMTPSession setupMockSession(final String argument,
             final boolean relaying, final boolean authRequired, final String user, final MailAddress recipient) {
        
        SMTPSession session = new BaseFakeSMTPSession() {

            HashMap<String,Object> connectionMap = new HashMap<String,Object>();
            HashMap<String,Object> map = new HashMap<String,Object>();
            
            public boolean isAuthSupported() {
                return authRequired;
            }
            
            public String getUser() {
                return user;
            }
            
            public Map<String,Object> getConnectionState() {
                return connectionMap;
            }
            
            public boolean isRelayingAllowed() {
                return relaying;
            }
            
            public Map<String,Object> getState() {
                return map;
            }
            
            
        };

        return session;
    }
    
    private DNSService setupMockDNSServer() {
    	DNSService dns = new BaseFakeDNSService(){
            public InetAddress getByName(String host) throws UnknownHostException {
                if (host.equals(INVALID_HOST)) 
                    throw new UnknownHostException();
                return InetAddress.getLocalHost();
            }
        };
        
        return dns;
    }
    
    public void testRejectInvalidHelo() throws ParseException {
        MailAddress mailAddress = new MailAddress("test@localhost");
        SMTPSession session = setupMockSession(INVALID_HOST,false,false,null,mailAddress);
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        handler.setDNSService(setupMockDNSServer());
        
        handler.doHelo(session, INVALID_HOST);
        assertNotNull("Invalid HELO",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        int result = handler.doRcpt(session,null, mailAddress).getResult();
        assertEquals("Reject", result,HookReturnCode.DENY);
    }
    
    
    public void testNotRejectValidHelo() throws ParseException {
        MailAddress mailAddress = new MailAddress("test@localhost");
        SMTPSession session = setupMockSession(VALID_HOST,false,false,null,mailAddress);
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
                
        handler.setDNSService(setupMockDNSServer());
  
        handler.doHelo(session, VALID_HOST);
        assertNull("Valid HELO",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));

        int result = handler.doRcpt(session,null, mailAddress).getResult();
        assertEquals("Not reject", result,HookReturnCode.DECLINED);
    }
   
    
    public void testRejectInvalidHeloAuthUser() throws ParseException {
        MailAddress mailAddress = new MailAddress("test@localhost");
        SMTPSession session = setupMockSession(INVALID_HOST,false,true,"valid@user",mailAddress);
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
                
        handler.setDNSService(setupMockDNSServer());

        handler.doHelo(session, INVALID_HOST);
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        int result = handler.doRcpt(session,null, mailAddress).getResult();
        assertEquals("Reject", result,HookReturnCode.DENY);
    }
    
   
    
    public void testRejectRelay() throws ParseException {
        MailAddress mailAddress = new MailAddress("test@localhost");
        SMTPSession session = setupMockSession(INVALID_HOST,true,false,null,mailAddress);
        ResolvableEhloHeloHandler handler = new ResolvableEhloHeloHandler();
        
        
        handler.setDNSService(setupMockDNSServer());

        handler.doHelo(session, INVALID_HOST);
        assertNotNull("Value stored",session.getState().get(ResolvableEhloHeloHandler.BAD_EHLO_HELO));
        
        
        int result = handler.doRcpt(session,null, mailAddress).getResult();
        assertEquals("Reject", result,HookReturnCode.DENY);
    }
}
    
