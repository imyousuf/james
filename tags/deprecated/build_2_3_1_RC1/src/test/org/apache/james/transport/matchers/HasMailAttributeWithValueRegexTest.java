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

import org.apache.james.test.mock.javaxmail.MockMimeMessage;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMatcherConfig;

import org.apache.mailet.MailAddress;
import org.apache.mailet.Matcher;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;
import javax.mail.internet.MimeMessage.RecipientType;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

public class HasMailAttributeWithValueRegexTest extends TestCase {

    private MimeMessage mockedMimeMessage;

    private MockMail mockedMail;

    private Matcher matcher;

    private final String MAIL_ATTRIBUTE_NAME = "org.apache.james.test.junit";

    private final String MAIL_ATTRIBUTE_VALUE = "true";

    private String mailAttributeName = "org.apache.james";

    private String mailAttributeValue = "false";

    private String regex = ".*";

    public HasMailAttributeWithValueRegexTest(String arg0)
            throws UnsupportedEncodingException {
        super(arg0);
    }

    private void setMailAttributeName(String mailAttributeName) {
        this.mailAttributeName = mailAttributeName;
    }

    private void setMailAttributeValue(String mailAttributeValue) {
        this.mailAttributeValue = mailAttributeValue;
    }

    private void setRegex(String regex) {
        this.regex = regex;
    }

    private void setupMockedMimeMessage() throws MessagingException {
        String sender = "test@james.apache.org";
        String rcpt = "test2@james.apache.org";

        mockedMimeMessage = new MockMimeMessage();
        mockedMimeMessage.setFrom(new InternetAddress(sender));
        mockedMimeMessage.setRecipients(RecipientType.TO, rcpt);
        mockedMimeMessage.setSubject("testmail");
        mockedMimeMessage.setText("testtext");
        mockedMimeMessage.saveChanges();

    }

    private void setupMockedMail(MimeMessage m) throws ParseException {
        mockedMail = new MockMail();
        mockedMail.setMessage(m);
        mockedMail.setRecipients(Arrays.asList(new MailAddress[] {
                new MailAddress("test@james.apache.org"),
                new MailAddress("test2@james.apache.org") }));
        mockedMail.setAttribute(mailAttributeName,
                (Serializable) mailAttributeValue);

    }

    private void setupMatcher() throws MessagingException {
        setupMockedMimeMessage();
        matcher = new HasMailAttributeWithValueRegex();
        MockMatcherConfig mci = new MockMatcherConfig("HasMailAttribute="
                + MAIL_ATTRIBUTE_NAME + ", " + regex, new MockMailContext());
        matcher.init(mci);
    }

    // test if the mail attribute was matched
    public void testAttributeIsMatched() throws MessagingException {
        setMailAttributeName(MAIL_ATTRIBUTE_NAME);
        setMailAttributeValue(MAIL_ATTRIBUTE_VALUE);
        setRegex(".*");

        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);
        setupMatcher();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNotNull(matchedRecipients);
        assertEquals(matchedRecipients.size(), mockedMail.getRecipients()
                .size());
    }

    // test if the mail attribute was not matched
    public void testHeaderIsNotMatched() throws MessagingException {
        setRegex("\\d");
        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);
        setupMatcher();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNull(matchedRecipients);
    }

    // test if an exception was thrown cause the regex was invalid
    public void testHeaderIsNotMatchedCauseValue() throws MessagingException {

        String invalidRegex = "(!(";
        String regexException = null;
        String exception = "Malformed pattern: " + invalidRegex;

        setRegex(invalidRegex);
        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);

        try {
            setupMatcher();
        } catch (MessagingException m) {
            regexException = m.getMessage();
        }

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNull(matchedRecipients);
        assertEquals(regexException, exception);

    }
}
