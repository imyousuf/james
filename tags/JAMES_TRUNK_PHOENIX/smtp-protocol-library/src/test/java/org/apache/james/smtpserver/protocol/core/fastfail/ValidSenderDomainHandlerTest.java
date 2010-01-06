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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.james.smtpserver.protocol.BaseFakeDNSService;
import org.apache.james.smtpserver.protocol.BaseFakeSMTPSession;
import org.apache.james.smtpserver.protocol.DNSService;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.core.fastfail.ValidSenderDomainHandler;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.mailet.MailAddress;

public class ValidSenderDomainHandlerTest extends TestCase {
    
    private DNSService setupDNSServer() {
    	DNSService dns = new BaseFakeDNSService(){

            public Collection<String> findMXRecords(String hostname) {
                Collection<String> mx = new ArrayList<String>();
                if (hostname.equals("test.james.apache.org")) {
                    mx.add("mail.james.apache.org");
                }
                return mx;
            }
            
        };
        return dns;
    }
    
    private SMTPSession setupMockedSession(final MailAddress sender) {
        SMTPSession session = new BaseFakeSMTPSession() {
            HashMap<String,Object> state = new HashMap<String,Object>();

            public Map<String,Object> getState() {

                state.put(SMTPSession.SENDER, sender);

                return state;
            }
            
            public boolean isRelayingAllowed() {
                return false;
            }

            
        };
        return session;
    }
    
    
    // Test for JAMES-580
    public void testNullSenderNotReject() {
        ValidSenderDomainHandler handler = new ValidSenderDomainHandler();
        handler.setDNSService(setupDNSServer());
        int response = handler.doMail(setupMockedSession(null),null).getResult();
        
        assertEquals("Not blocked cause its a nullsender",response,HookReturnCode.DECLINED);
    }

    public void testInvalidSenderDomainReject() throws ParseException {
        ValidSenderDomainHandler handler = new ValidSenderDomainHandler();
        SMTPSession session = setupMockedSession(new MailAddress("invalid@invalid"));
        handler.setDNSService(setupDNSServer());
        int response = handler.doMail(session,(MailAddress) session.getState().get(SMTPSession.SENDER)).getResult();
        
        assertEquals("Blocked cause we use reject action", response,HookReturnCode.DENY);
    }
}
