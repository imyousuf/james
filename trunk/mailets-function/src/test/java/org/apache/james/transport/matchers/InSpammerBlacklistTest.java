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


package org.apache.james.transport.matchers;

import org.apache.james.Constants;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.mailet.base.test.MockMail;
import org.apache.mailet.base.test.MockMailContext;
import org.apache.mailet.base.test.MockMatcherConfig;

import org.apache.mailet.MailAddress;
import org.apache.mailet.Matcher;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

public class InSpammerBlacklistTest extends TestCase {

    private MockMail mockedMail;

    private Matcher matcher;
    
    private final static String BLACKLIST = "my.black.list.";
    private final static StringBuffer LISTED_HOST = new StringBuffer("111.222.111.222");

    public InSpammerBlacklistTest(String arg0) throws UnsupportedEncodingException {
        super(arg0);
    }

    private MockServiceManager setUpServiceManager() {
        MockServiceManager sMan = new MockServiceManager();
        sMan.put(DNSService.ROLE, setUpDNSServer());
        return sMan;
    }
    
    private DNSService setUpDNSServer() {
        DNSService dns = new AbstractDNSServer() {
            public InetAddress getByName(String name) throws UnknownHostException {
                if (name.equals(LISTED_HOST.reverse() + "." + BLACKLIST)) {
                    return null;
                } else {
                    throw new UnknownHostException("Not listed");
                }
            }
        };
        return dns;
    }
    private void setupMockedMail(String remoteAddr) throws ParseException {
        mockedMail = new MockMail();
        mockedMail.setRemoteAddr(remoteAddr);
        mockedMail.setRecipients(Arrays.asList(new MailAddress[] {new MailAddress("test@email")}));

    }

    private void setupMatcher(String blacklist) throws MessagingException {
        matcher = new InSpammerBlacklist();
        MockMailContext context = new MockMailContext();
        context.setAttribute(Constants.AVALON_COMPONENT_MANAGER, setUpServiceManager());
        MockMatcherConfig mci = new MockMatcherConfig("InSpammerBlacklist=" + blacklist,context);
        matcher.init(mci);
    }

    
    public void testInBlackList() throws MessagingException {
        setupMockedMail(LISTED_HOST.toString());
        setupMatcher(BLACKLIST);

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNotNull(matchedRecipients);
        assertEquals(matchedRecipients.size(), mockedMail.getRecipients().size());
    }
    
    public void testNotInBlackList() throws MessagingException {
        setupMockedMail("212.12.14.1");
        setupMatcher(BLACKLIST);

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNull(matchedRecipients);
    }
}
