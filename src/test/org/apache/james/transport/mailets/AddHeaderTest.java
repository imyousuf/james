/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.transport.mailets;

import org.apache.james.test.mock.javaxmail.MockMimeMessage;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMailetConfig;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mailet;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import junit.framework.TestCase;

public class AddHeaderTest extends TestCase {

    private MimeMessage mockedMimeMessage;

    private Mail mockedMail;

    private Mailet mailet;

    private final String HEADER_NAME = "JUNIT";

    private final String HEADER_VALUE = "test-value";

    private String headerName = "defaultHeaderName";

    private String headerValue = "defaultHeaderValue";

    public AddHeaderTest(String arg0) throws UnsupportedEncodingException {
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

    private void setupMockedMail() {
        mockedMail = new Mail() {

            private static final long serialVersionUID = 1L;

            public String getName() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setName(String newName) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public MimeMessage getMessage() throws MessagingException {
                return mockedMimeMessage;
            }

            public Collection getRecipients() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setRecipients(Collection recipients) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public MailAddress getSender() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getState() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteHost() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteAddr() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getErrorMessage() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setErrorMessage(String msg) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setMessage(MimeMessage message) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setState(String state) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Serializable getAttribute(String name) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Iterator getAttributeNames() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean hasAttributes() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Serializable removeAttribute(String name) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void removeAllAttributes() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Serializable setAttribute(String name, Serializable object) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public long getMessageSize() throws MessagingException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Date getLastUpdated() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setLastUpdated(Date lastUpdated) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

        };
    }

    private void setupMailet() throws MessagingException {
        setupMockedMimeMessage();
        mailet = new AddHeader();
        MockMailetConfig mci = new MockMailetConfig("Test",
                new MockMailContext());
        mci.setProperty("name", HEADER_NAME);
        mci.setProperty("value", HEADER_VALUE);

        mailet.init(mci);
    }

    // test if the Header was add
    public void testHeaderIsPresent() throws MessagingException {
        setupMockedMail();
        setupMockedMimeMessage();
        setupMailet();

        mailet.service(mockedMail);

        assertEquals(HEADER_VALUE, mockedMail.getMessage().getHeader(
                HEADER_NAME)[0]);

    }

    // test if the Header was replaced
    public void testHeaderIsReplaced() throws MessagingException {
        setHeaderName(HEADER_NAME);
        setHeaderValue(headerValue);
        setupMockedMail();
        setupMockedMimeMessage();
        setupMailet();

        mailet.service(mockedMail);

        assertEquals(HEADER_VALUE, mockedMail.getMessage().getHeader(
                HEADER_NAME)[0]);

    }
}
