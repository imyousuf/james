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

import java.io.Serializable;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMatcherConfig;
import org.apache.james.test.mock.util.MailUtil;
import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Matcher;

public abstract class AbstractHasMailAttributeTest extends TestCase {
    protected MimeMessage mockedMimeMessage;

    protected MockMail mockedMail;

    protected Matcher matcher;

    protected final String MAIL_ATTRIBUTE_NAME = "org.apache.james.test.junit";

    protected final String MAIL_ATTRIBUTE_VALUE = "true";

    protected String mailAttributeName = "org.apache.james";

    protected String mailAttributeValue = "false";

    public AbstractHasMailAttributeTest() {
        super(null);
    }

    protected void setMailAttributeName(String mailAttributeName) {
        this.mailAttributeName = mailAttributeName;
    }

    protected void setMailAttributeValue(String mailAttributeValue) {
        this.mailAttributeValue = mailAttributeValue;
    }

    protected void setupMockedMail(MimeMessage m) throws ParseException {
        mockedMail = MailUtil.createMockMail2Recipients(m);
        mockedMail.setAttribute(mailAttributeName,
                (Serializable) mailAttributeValue);
    }

    protected void setupMatcher() throws MessagingException {
        matcher = createMatcher();
        MockMatcherConfig mci = new MockMatcherConfig(getConfigOption()
                + getHasMailAttribute(), new MockMailContext());
        matcher.init(mci);
    }

    // test if the mail attribute was matched
    public void testAttributeIsMatched() throws MessagingException {
        init();

        setupAll();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNotNull(matchedRecipients);
        assertEquals(matchedRecipients.size(), mockedMail.getRecipients()
                .size());
    }

    protected void init() {
        setMailAttributeName(MAIL_ATTRIBUTE_NAME);
        setMailAttributeValue(MAIL_ATTRIBUTE_VALUE);
    }

    protected void setupAll() throws MessagingException {
        setupMockedMail(mockedMimeMessage);
        setupMatcher();
    }

    // test if the mail attribute was not matched
    public void testAttributeIsNotMatched() throws MessagingException {
        setupAll();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNull(matchedRecipients);
    }

    protected abstract String getHasMailAttribute();

    protected abstract GenericMatcher createMatcher();

    protected abstract String getConfigOption();
}
