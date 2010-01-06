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

import java.util.ArrayList;

import javax.mail.internet.ParseException;

import org.apache.james.smtpserver.protocol.BaseFakeSMTPSession;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.core.fastfail.SpamTrapHandler;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.mailet.MailAddress;

import junit.framework.TestCase;

public class SpamTrapHandlerTest extends TestCase {
    private static final String SPAM_TRAP_RECIP1 = "spamtrap1@localhost";
    private static final String RECIP1 = "recip@localhost";
    
    private SMTPSession setUpSMTPSession(final String ip) {
        return new BaseFakeSMTPSession() {
            public String getRemoteIPAddress() {
                return ip;
            }
        
        };
    }
    
    public void testSpamTrap() throws ParseException {
        String ip = "192.168.100.1";
        String ip2 = "192.168.100.2";
        long blockTime = 2000;
    
        ArrayList<String> rcpts = new ArrayList<String>();
        rcpts.add(SPAM_TRAP_RECIP1);
    
        SpamTrapHandler handler = new SpamTrapHandler();
    
        handler.setBlockTime(blockTime);
        handler.setSpamTrapRecipients(rcpts);
    
        int result = handler.doRcpt(setUpSMTPSession(ip),null,new MailAddress(SPAM_TRAP_RECIP1)).getResult();
    
        assertEquals("Blocked on first connect",HookReturnCode.DENY,result);
    

        result = handler.doRcpt(setUpSMTPSession(ip),null,new MailAddress(RECIP1)).getResult();
    
        assertEquals("Blocked on second connect", HookReturnCode.DENY,result);
    
        
        result = handler.doRcpt(setUpSMTPSession(ip2),null,new MailAddress(RECIP1)).getResult();
    
        assertEquals("Not Blocked", HookReturnCode.DECLINED,result);
    
        try {
            // Wait for the blockTime to exceed
            Thread.sleep(blockTime);
        } catch (InterruptedException e) {
            fail("Failed to sleep for " + blockTime +" ms");
        }
    
        result = handler.doRcpt(setUpSMTPSession(ip),null,new MailAddress(RECIP1)).getResult();
    
        assertEquals("Not blocked. BlockTime exceeded", HookReturnCode.DECLINED,result); 
    }
}
