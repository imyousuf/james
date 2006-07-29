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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.services.DNSServer;
import org.apache.james.smtpserver.core.filter.fastfail.ValidSenderDomainHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.mailet.MailAddress;

import junit.framework.TestCase;

public class ValidSenderDomainHandlerTest extends TestCase {
    
    private String response = null;

    public void setUp() {
        response = null;
    }
    
    private DNSServer setupDNSServer() {
        DNSServer dns = new DNSServer(){

            public Collection findMXRecords(String hostname) {
                Collection mx = new ArrayList();
                if (hostname.equals("test.james.apache.org")) {
                    mx.add("mail.james.apache.org");
                }
                return mx;
            }

            public Collection findTXTRecords(String hostname) {
                throw new UnsupportedOperationException("Unimplemented mock service");
            }

            public InetAddress[] getAllByName(String host) throws UnknownHostException {
                throw new UnsupportedOperationException("Unimplemented mock service");
            }

            public InetAddress getByName(String host) throws UnknownHostException {
                throw new UnsupportedOperationException("Unimplemented mock service");
            }

            public Iterator getSMTPHostAddresses(String domainName) {
                throw new UnsupportedOperationException("Unimplemented mock service");
            }
            
        };
        return dns;
    }
    
    private SMTPSession setupMockedSession(final MailAddress sender) {
        SMTPSession session = new AbstractSMTPSession() {
            HashMap state = new HashMap();
            
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
            
        };
        return session;
    }
    
    private String getResponse() {
        return response;
    }
    
    // Test for JAMES-580
    public void testNullSender() {
        ValidSenderDomainHandler handler = new ValidSenderDomainHandler();
        ContainerUtil.enableLogging(handler, new MockLogger());
        
        handler.setDnsServer(setupDNSServer());
        handler.onCommand(setupMockedSession(null));
        
        assertNull("Not blocked cause its a nullsender",getResponse());
    }
}
