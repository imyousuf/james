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

import junit.framework.TestCase;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.smtpserver.core.filter.fastfail.MaxRcptHandler;
import org.apache.james.smtpserver.hook.RcptHook;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.mailet.MailAddress;



public class MaxRcptHandlerTest extends TestCase{
    
    private SMTPSession setupMockedSession(final int rcptCount) {
        SMTPSession session = new AbstractSMTPSession() {
            HashMap state = new HashMap();

            public Map getState() {
                return state;
            }
            
            public boolean isRelayingAllowed() {
                return false;
            }
            
            public int getRcptCount() {
                return rcptCount;
            }
            
        };
        return session;
    }
    
    public void testRejectMaxRcpt() throws ParseException {
        SMTPSession session = setupMockedSession(3);
        MaxRcptHandler handler = new MaxRcptHandler();
    
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        //handler.setAction("reject");
        handler.setMaxRcpt(2);
        int resp = handler.doRcpt(session,null,new MailAddress("test@test")).getResult();
    
        assertEquals("Rejected.. To many recipients", resp, RcptHook.DENY);
    }
    /*
    public void testAddScoreMaxRcpt() {
        SMTPSession session = setupMockedSession(3);
        session.getState().put(JunkScore.JUNK_SCORE, new JunkScoreImpl());
    
        MaxRcptHandler handler = new MaxRcptHandler();
    
        ContainerUtil.enableLogging(handler,new MockLogger());
    
        handler.setAction("junkScore");
        handler.setScore(20);
        handler.setMaxRcpt(2);
        SMTPResponse response = handler.onCommand(session,"RCPT","<test@test>");

        assertNull("Not Rejected.. we use junkScore action", response);
        assertEquals("Get Score", ((JunkScore) session.getState().get(JunkScore.JUNK_SCORE)).getStoredScore("MaxRcptCheck"),20.0,0d);
    }
    */
    
    public void testNotRejectMaxRcpt() throws ParseException {
        SMTPSession session = setupMockedSession(3);
        MaxRcptHandler handler = new MaxRcptHandler();
    
        ContainerUtil.enableLogging(handler,new MockLogger());
    

        handler.setMaxRcpt(4);
        int resp = handler.doRcpt(session,null,new MailAddress("test@test")).getResult();
        
        assertEquals("Not Rejected..", resp, RcptHook.OK);
    }

}
