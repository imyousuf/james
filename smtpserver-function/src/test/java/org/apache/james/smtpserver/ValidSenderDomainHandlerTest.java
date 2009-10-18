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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.core.fastfail.ValidSenderDomainHandler;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.mailet.MailAddress;

public class ValidSenderDomainHandlerTest extends TestCase {
    
    private DNSService setupDNSServer() {
    	DNSService dns = new AbstractDNSServer(){

            public Collection findMXRecords(String hostname) {
                Collection mx = new ArrayList();
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
            HashMap state = new HashMap();

            public Map getState() {

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
        ContainerUtil.enableLogging(handler, new MockLogger());
        
        handler.setDNSService(setupDNSServer());
        int response = handler.doMail(setupMockedSession(null),null).getResult();
        
        assertEquals("Not blocked cause its a nullsender",response,HookReturnCode.DECLINED);
    }

    public void testInvalidSenderDomainReject() throws ParseException {
        ValidSenderDomainHandler handler = new ValidSenderDomainHandler();
        SMTPSession session = setupMockedSession(new MailAddress("invalid@invalid"));
        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setDNSService(setupDNSServer());
        int response = handler.doMail(session,(MailAddress) session.getState().get(SMTPSession.SENDER)).getResult();
        
        assertEquals("Blocked cause we use reject action", response,HookReturnCode.DENY);
    }
}
