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

import java.io.UnsupportedEncodingException;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMatcherConfig;
import org.apache.james.test.util.Util;
import org.apache.mailet.Matcher;

public class SMTPIsAuthNetworkTest extends TestCase {

    private MockMail mockedMail;

    private Matcher matcher;

    private boolean isAuthorized = false;

    private final String MAIL_ATTRIBUTE_NAME = "org.apache.james.SMTPIsAuthNetwork";

    public SMTPIsAuthNetworkTest(String arg0)
            throws UnsupportedEncodingException {
        super(arg0);
    }

    private void setIsAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
    }

    private void setupMockedMail() throws ParseException {
        mockedMail = Util.createMockMail2Recipients(null);
        if (isAuthorized) {
            mockedMail.setAttribute(MAIL_ATTRIBUTE_NAME, "true");
        }
    }

    private void setupMatcher() throws MessagingException {
        matcher = new SMTPIsAuthNetwork();
        MockMatcherConfig mci = new MockMatcherConfig("SMTPIsAuthNetwork",
                new MockMailContext());
        matcher.init(mci);
    }

    public void testIsAuthNetwork() throws MessagingException {
        setIsAuthorized(true);
        setupMockedMail();
        setupMatcher();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNotNull(matchedRecipients);
        assertEquals(matchedRecipients.size(), mockedMail.getRecipients()
                .size());
    }

    public void testIsNotAuthNetwork() throws MessagingException {
        setIsAuthorized(false);
        setupMockedMail();
        setupMatcher();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNull(matchedRecipients);
    }
}
