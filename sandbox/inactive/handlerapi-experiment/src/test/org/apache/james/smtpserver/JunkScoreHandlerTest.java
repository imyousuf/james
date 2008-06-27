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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.smtpserver.core.filter.fastfail.JunkScoreHandler;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.javaxmail.MockMimeMessage;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.mailet.Mail;

import junit.framework.TestCase;

public class JunkScoreHandlerTest extends TestCase {

    private SMTPSession setupMockedSMTPSession() {
        SMTPSession session = new AbstractSMTPSession() {
            HashMap state = new HashMap();
            HashMap cState = new HashMap();
        
            public Map getState() {
                state.put(SMTPSession.SENDER, "sender@localhost");
                return state;
            }

            public Map getConnectionState() {
                return cState;
            }
            
            public String getRemoteHost() {
                return "anyHost";
            }
            
            public String getRemoteIPAddress() {
                return "000.000.000.001";
            }
        };
        return session;
    }
    
    private Mail getMockMail(){
        Mail m = new MockMail();
        try {
            m.setMessage(new MockMimeMessage());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return m;
    }
    
    public void testIllegalActionThrowException() {
        boolean exception = false;
        JunkScoreHandler handler = new JunkScoreHandler();
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        try {
            handler.setAction("invalid");
        } catch (ConfigurationException e) {
            exception = true;
        }
    
        assertTrue("Exception thrown",exception);
    }
    
    public void testRejectAction() throws ConfigurationException {
        Map mappings = new HashMap();
        Object c = getDummyClass();
        Object o = new Object().getClass();
        
        mappings.put(c.getClass().getName(), "20.0");
        
        SMTPSession session = setupMockedSMTPSession();
        JunkScoreHandler handler = new JunkScoreHandler();
        ContainerUtil.enableLogging(handler,new MockLogger());
        
        handler.setAction("reject");
        handler.setMaxScore(15.0);
        Mail m = getMockMail();
        handler.onMessage(session, m);

        handler.setScoreMappings(mappings);
        
        assertEquals("HookResult not changed", handler.onHookResult(session, new HookResult(HookReturnCode.DENY), o).getResult(), HookReturnCode.DENY);

        assertEquals("HookResult changed", handler.onHookResult(session, new HookResult(HookReturnCode.DENY), c).getResult(), HookReturnCode.DECLINED);

        assertEquals("Rejected", handler.onMessage(session, m).getResult(), HookReturnCode.DENY);
    }

    
    public void testHeaderAction() throws ConfigurationException, MessagingException {
        Map mappings = new HashMap();
        Object c = getDummyClass();
        
        mappings.put(c.getClass().getName(), "20.0");
        
        SMTPSession session = setupMockedSMTPSession();
        JunkScoreHandler handler = new JunkScoreHandler();
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.setAction("header");
        handler.setScoreMappings(mappings);

        Mail m = getMockMail();
        assertEquals("HookResult changed", handler.onHookResult(session, new HookResult(HookReturnCode.DENY), c).getResult(), HookReturnCode.DECLINED);

        assertEquals("Not rejected", handler.onMessage(session,m).getResult(),HookReturnCode.DECLINED);
    
        MimeMessage message = m.getMessage();
        assertNotNull("Header added",message.getHeader("X-JUNKSCORE")[0]);
    }
    
    
    private DummyClass getDummyClass() {
        return new DummyClass();
    }
    
    private class DummyClass{
        // just a dummy class for testing
    }
    
}
