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

package org.apache.james.transport.mailets;

import org.apache.james.test.mock.javaxmail.MockMimeMessage;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMailetConfig;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mailet;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;
import javax.mail.internet.MimeMessage.RecipientType;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import junit.framework.TestCase;

public class SetMimeHeaderTest extends TestCase {

    private MimeMessage mockedMimeMessage;

    private Mail mockedMail;

    private Mailet mailet;

    private final String HEADER_NAME = "JUNIT";

    private final String HEADER_VALUE = "test-value";

    private String headerName = "defaultHeaderName";

    private String headerValue = "defaultHeaderValue";

    public SetMimeHeaderTest(String arg0) throws UnsupportedEncodingException {
        super(arg0);
    }

    private void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    private void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }

    private void setupMockedMimeMessage() throws MessagingException {
        String sender = "test@james.apache.org";
        String rcpt = "test2@james.apache.org";

        mockedMimeMessage = new MockMimeMessage();
        mockedMimeMessage.setFrom(new InternetAddress(sender));
        mockedMimeMessage.setRecipients(RecipientType.TO, rcpt);
        mockedMimeMessage.setHeader(headerName, headerValue);
        mockedMimeMessage.setSubject("testmail");
        mockedMimeMessage.setText("testtext");
        mockedMimeMessage.saveChanges();

    }

    private void setupMockedMail(MimeMessage m) throws ParseException {
        mockedMail = new MockMail();
        mockedMail.setMessage(m);
        mockedMail.setRecipients(Arrays.asList(new MailAddress[] {
                new MailAddress("test@james.apache.org"), new MailAddress("test2@james.apache.org") }));

    }

    private void setupMailet() throws MessagingException {
        setupMockedMimeMessage();
        mailet = new SetMimeHeader();
        MockMailetConfig mci = new MockMailetConfig("Test",
                new MockMailContext());
        mci.setProperty("name", HEADER_NAME);
        mci.setProperty("value", HEADER_VALUE);

        mailet.init(mci);
    }

    // test if the Header was add
    public void testHeaderIsPresent() throws MessagingException {
        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);
        setupMailet();

        mailet.service(mockedMail);

        assertEquals(HEADER_VALUE, mockedMail.getMessage().getHeader(
                HEADER_NAME)[0]);

    }

    // test if the Header was replaced
    public void testHeaderIsReplaced() throws MessagingException {
        setHeaderName(HEADER_NAME);
        setHeaderValue(headerValue);

        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);
        setupMailet();

        mailet.service(mockedMail);

        assertEquals(HEADER_VALUE, mockedMail.getMessage().getHeader(
                HEADER_NAME)[0]);

    }
}
