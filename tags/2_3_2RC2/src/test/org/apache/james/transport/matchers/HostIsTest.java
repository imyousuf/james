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
import javax.mail.internet.MimeMessage.RecipientType;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

public class HostIsTest extends TestCase {

    private MimeMessage mockedMimeMessage;

    private MockMail mockedMail;

    private Matcher matcher;

    private final String HOST_NAME = "james.apache.org";

    private MailAddress[] recipients;

    public HostIsTest(String arg0) throws UnsupportedEncodingException {
        super(arg0);
    }

    private void setRecipients(MailAddress[] recipients) {
        this.recipients = recipients;
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

    private void setupMockedMail(MimeMessage m) {
        mockedMail = new MockMail();
        mockedMail.setMessage(m);
        mockedMail.setRecipients(Arrays.asList(recipients));

    }

    private void setupMatcher() throws MessagingException {
        setupMockedMimeMessage();
        matcher = new HostIs();
        MockMatcherConfig mci = new MockMatcherConfig("HostIs=" + HOST_NAME,
                new MockMailContext());
        matcher.init(mci);
    }

    // test if all recipients get returned as matched
    public void testHostIsMatchedAllRecipients() throws MessagingException {
        setRecipients(new MailAddress[] {
                new MailAddress("test@james.apache.org"),
                new MailAddress("test2@james.apache.org") });

        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);
        setupMatcher();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNotNull(matchedRecipients);
        assertEquals(matchedRecipients.size(), mockedMail.getRecipients()
                .size());
    }

    // test if one recipients get returned as matched
    public void testHostIsMatchedOneRecipient() throws MessagingException {
        setRecipients(new MailAddress[] {
                new MailAddress("test@james2.apache.org"),
                new MailAddress("test2@james.apache.org") });

        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);
        setupMatcher();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertNotNull(matchedRecipients);
        assertEquals(matchedRecipients.size(), 1);
    }

    // test if no recipient get returned cause it not match
    public void testHostIsNotMatch() throws MessagingException {
        setRecipients(new MailAddress[] {
                new MailAddress("test@james2.apache.org"),
                new MailAddress("test2@james2.apache.org") });

        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);
        setupMatcher();

        Collection matchedRecipients = matcher.match(mockedMail);

        assertEquals(matchedRecipients.size(), 0);
    }
}
