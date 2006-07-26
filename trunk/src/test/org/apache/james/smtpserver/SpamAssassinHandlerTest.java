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
import java.util.HashMap;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.smtpserver.core.filter.fastfail.SpamAssassinHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.javaxmail.MockMimeMessage;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.test.mock.util.MockSpamd;
import org.apache.james.test.util.Util;
import org.apache.james.util.SpamAssassinInvoker;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.mailet.Mail;

public class SpamAssassinHandlerTest extends TestCase {
    private SMTPSession mockedSMTPSession;

    private String response = null;
    
    public final static String SPAMD_HOST = "localhost";

    public void setUp() {
        // reset reponse
        response = null;
    }

    private SMTPSession setupMockedSMTPSession(final Mail mail) {
        mockedSMTPSession = new SMTPSession() {

            private HashMap state = new HashMap();

            private String ipAddress = "192.168.0.1";

            private String host = "localhost";

            private boolean relayingAllowed;

            public void abortMessage() {
            }

            public String clearResponseBuffer() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void endSession() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getCommandArgument() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getCommandName() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public SMTPHandlerConfigurationData getConfigurationData() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public HashMap getConnectionState() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public InputStream getInputStream() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Mail getMail() {
                return mail;
            }

            public int getRcptCount() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteHost() {
                return host;
            }

            public String getRemoteIPAddress() {
                return ipAddress;
            }

            public StringBuffer getResponseBuffer() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getSessionID() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public HashMap getState() {
                state.put(SMTPSession.SENDER, "sender@james.apache.org");
                return state;
            }

            public boolean getStopHandlerProcessing() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getUser() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public Watchdog getWatchdog() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean isAuthRequired() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean isRelayingAllowed() {
                return relayingAllowed;
            }

            public boolean isSessionEnded() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String readCommandLine() throws IOException {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void resetConnectionState() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void resetState() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setMail(Mail mail) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setRelayingAllowed(boolean relayingAllowed) {
                this.relayingAllowed = relayingAllowed;
            }

            public void setStopHandlerProcessing(boolean b) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void setUser(String user) {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public boolean useHeloEhloEnforcement() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public void writeResponse(String respString) {
                response = respString;
            }
        };

        return mockedSMTPSession;

    }

    private String getResponse() {
        return response;
    }

    private Mail setupMockedMail(MimeMessage message) {
        MockMail mail = new MockMail();
        mail.setMessage(message);
        return mail;
    }

    public MimeMessage setupMockedMimeMessage(String text)
            throws MessagingException {
        MimeMessage message = new MimeMessage(new MockMimeMessage());
        message.setText(text);
        message.saveChanges();

        return message;
    }

    public void testNonSpam() throws IOException, MessagingException {

        int port = Util.getNonPrivilegedPort();
        MockSpamd spamd = new MockSpamd(port);
        new Thread(spamd).start();

        SMTPSession session = setupMockedSMTPSession(setupMockedMail(setupMockedMimeMessage("test")));

        SpamAssassinHandler handler = new SpamAssassinHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setSpamdHost(SPAMD_HOST);
        handler.setSpamdPort(port);
        handler.setSpamdRejectionHits(200.0);
        handler.onMessage(session);

        assertNull("Email was not rejected", getResponse());
        assertEquals("email was not spam", session.getMail().getAttribute(
                SpamAssassinInvoker.FLAG_MAIL_ATTRIBUTE_NAME), "NO");
        assertNotNull("spam hits", session.getMail().getAttribute(
                SpamAssassinInvoker.STATUS_MAIL_ATTRIBUTE_NAME));

    }

    public void testSpam() throws IOException, MessagingException {

        int port = Util.getNonPrivilegedPort();
        new Thread(new MockSpamd(port)).start();

        SMTPSession session = setupMockedSMTPSession(setupMockedMail(setupMockedMimeMessage(MockSpamd.GTUBE)));

        SpamAssassinHandler handler = new SpamAssassinHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setSpamdHost(SPAMD_HOST);
        handler.setSpamdPort(port);
        handler.setSpamdRejectionHits(2000.0);
        handler.onMessage(session);

        assertNull("Email was not rejected", getResponse());
        assertEquals("email was spam", session.getMail().getAttribute(
                SpamAssassinInvoker.FLAG_MAIL_ATTRIBUTE_NAME), "YES");
        assertNotNull("spam hits", session.getMail().getAttribute(
                SpamAssassinInvoker.STATUS_MAIL_ATTRIBUTE_NAME));
    }

    public void testSpamReject() throws IOException, MessagingException {

        int port = Util.getNonPrivilegedPort();
        new Thread(new MockSpamd(port)).start();

        SMTPSession session = setupMockedSMTPSession(setupMockedMail(setupMockedMimeMessage(MockSpamd.GTUBE)));

        SpamAssassinHandler handler = new SpamAssassinHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setSpamdHost(SPAMD_HOST);
        handler.setSpamdPort(port);
        handler.setSpamdRejectionHits(200.0);
        handler.onMessage(session);

        assertNotNull("Email was rejected", getResponse());
        assertEquals("email was spam", session.getMail().getAttribute(
                SpamAssassinInvoker.FLAG_MAIL_ATTRIBUTE_NAME), "YES");
        assertNotNull("spam hits", session.getMail().getAttribute(
                SpamAssassinInvoker.STATUS_MAIL_ATTRIBUTE_NAME));
    }

}
