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

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.james.smtpserver.protocol.BaseFakeSMTPSession;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.core.fastfail.MaxRcptHandler;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.mailet.MailAddress;



public class MaxRcptHandlerTest extends TestCase{
    
    private SMTPSession setupMockedSession(final int rcptCount) {
        SMTPSession session = new BaseFakeSMTPSession() {
            HashMap<String,Object> state = new HashMap<String,Object>();

            public Map<String,Object> getState() {
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
        
        handler.setMaxRcpt(2);
        int resp = handler.doRcpt(session,null,new MailAddress("test@test")).getResult();
    
        assertEquals("Rejected.. To many recipients", resp, HookReturnCode.DENY);
    }
  
    
    public void testNotRejectMaxRcpt() throws ParseException {
        SMTPSession session = setupMockedSession(3);
        MaxRcptHandler handler = new MaxRcptHandler();    

        handler.setMaxRcpt(4);
        int resp = handler.doRcpt(session,null,new MailAddress("test@test")).getResult();
        
        assertEquals("Not Rejected..", resp, HookReturnCode.DECLINED);
    }

}
