/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.                  *
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

package org.apache.james.smtpserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.javaxmail.MockMimeMessage;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.mailet.Mail;

import junit.framework.TestCase;

public class AddHeaderHandlerTest extends TestCase {

    private SMTPSession mockedSMTPSession;

    private MimeMessage mockedMimeMessage;

    private Mail mockedMail;

    private final String HEADER_NAME = "JUNIT";

    private final String HEADER_VALUE = "test-value";

    private String headerName = "defaultHeaderName";

    private String headerValue = "defaultHeaderValue";

    protected void setUp() throws Exception {
        super.setUp();
        setupMockedSMTPSession();
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

    private void setupMockedMail(MimeMessage m) {
        mockedMail = new MockMail();
        mockedMail.setMessage(m);
        mockedMail.setRecipients(Arrays.asList(new String[] {
                "test@james.apache.org", "test2@james.apache.org" }));

    }

    private void setupMockedSMTPSession() {
        mockedSMTPSession = new SMTPSession() {

            public void writeResponse(String respString) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String readCommandLine() throws IOException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public StringBuffer getResponseBuffer() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String clearResponseBuffer() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public InputStream getInputStream() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getCommandName() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getCommandArgument() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Mail getMail() {
                return mockedMail;
            }

            public void setMail(Mail mail) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteHost() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteIPAddress() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void abortMessage() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void endSession() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean isSessionEnded() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public HashMap getState() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void resetState() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public SMTPHandlerConfigurationData getConfigurationData() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setBlockListed(boolean blocklisted) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean isBlockListed() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setBlockListedDetail(String detail) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getBlockListedDetail() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean isRelayingAllowed() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean isAuthRequired() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean useHeloEhloEnforcement() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getUser() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setUser(String user) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Watchdog getWatchdog() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getSessionID() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }
        };
    }

    // test if the Header was add
    public void testHeaderIsPresent() throws MessagingException {
        setHeaderName(HEADER_NAME);
        setHeaderValue(HEADER_VALUE);

        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);

        SetMimeHeaderHandler header = new SetMimeHeaderHandler();

        ContainerUtil.enableLogging(header, new MockLogger());

        header.setHeaderName(HEADER_NAME);
        header.setHeaderValue(HEADER_VALUE);
        header.onMessage(mockedSMTPSession);

        assertEquals(HEADER_VALUE, mockedSMTPSession.getMail().getMessage()
                .getHeader(HEADER_NAME)[0]);
    }

    // test if the Header was replaced
    public void testHeaderIsReplaced() throws MessagingException {
        setHeaderName(HEADER_NAME);
        setHeaderValue(headerValue);

        setupMockedMimeMessage();
        setupMockedMail(mockedMimeMessage);

        SetMimeHeaderHandler header = new SetMimeHeaderHandler();

        ContainerUtil.enableLogging(header, new MockLogger());

        header.setHeaderName(HEADER_NAME);
        header.setHeaderValue(HEADER_VALUE);
        header.onMessage(mockedSMTPSession);

        assertEquals(HEADER_VALUE, mockedSMTPSession.getMail().getMessage()
                .getHeader(HEADER_NAME)[0]);
    }

}
