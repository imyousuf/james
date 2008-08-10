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

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.api.dnsservice.DNSServer;
import org.apache.james.services.AbstractDNSServer;
import org.apache.james.smtpserver.core.filter.fastfail.ValidSenderDomainHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.util.junkscore.JunkScore;
import org.apache.james.util.junkscore.JunkScoreImpl;
import org.apache.mailet.MailAddress;

import junit.framework.TestCase;

public class ValidSenderDomainHandlerTest extends TestCase {
    
    private String response = null;

    public void setUp() {
        response = null;
    }
    
    private DNSServer setupDNSServer() {
        DNSServer dns = new AbstractDNSServer(){

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
        SMTPSession session = new AbstractSMTPSession() {
            HashMap state = new HashMap();
            boolean processing = false;
            
            public Map getState() {

                state.put(SMTPSession.SENDER, sender);

                return state;
            }
            
            public boolean isRelayingAllowed() {
                return false;
            }
            
            public void writeResponse(String resp) {
                response = resp;
            }
            
            public void setStopHandlerProcessing(boolean processing) {
                this.processing = processing;
            }
            
            public boolean getStopHandlerProcessing() {
                return processing;
            }
            
        };
        return session;
    }
    
    private String getResponse() {
        return response;
    }
    
    // Test for JAMES-580
    public void testNullSenderNotReject() {
        ValidSenderDomainHandler handler = new ValidSenderDomainHandler();
        ContainerUtil.enableLogging(handler, new MockLogger());
        
        handler.setDnsServer(setupDNSServer());
        handler.onCommand(setupMockedSession(null));
        
        assertNull("Not blocked cause its a nullsender",getResponse());
    }
    
    public void testInvalidSenderDomainAddJunkScore() throws ParseException {
        ValidSenderDomainHandler handler = new ValidSenderDomainHandler();
        SMTPSession session = setupMockedSession(new MailAddress("invalid@invalid"));
        ContainerUtil.enableLogging(handler, new MockLogger());
        session.getState().put(JunkScore.JUNK_SCORE, new JunkScoreImpl());
        handler.setAction("JunkScore");
        handler.setScore(20);
        handler.setDnsServer(setupDNSServer());
        handler.onCommand(session);
        
        assertNull("Not blocked cause we use JunkScore",getResponse());
        assertEquals("JunkScore is stored",((JunkScore) session.getState().get(JunkScore.JUNK_SCORE)).getStoredScore("ValidSenderDomainCheck"),20.0,0d);
    }
    
    public void testInvalidSenderDomainReject() throws ParseException {
        ValidSenderDomainHandler handler = new ValidSenderDomainHandler();
        SMTPSession session = setupMockedSession(new MailAddress("invalid@invalid"));
        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setDnsServer(setupDNSServer());
        handler.onCommand(session);
        
        assertNotNull("Blocked cause we use reject action",getResponse());
    }
}
