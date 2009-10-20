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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.smtpserver.integration.SpamAssassinHandler;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.james.test.mock.MockMimeMessage;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.test.mock.util.MockSpamd;
import org.apache.james.test.util.Util;
import org.apache.james.util.scanner.SpamAssassinInvoker;
import org.apache.mailet.Mail;
 
public class SpamAssassinHandlerTest extends TestCase {
    private SMTPSession mockedSMTPSession;

    private Mail mockedMail;
 
    public final static String SPAMD_HOST = "localhost";

    private SMTPSession setupMockedSMTPSession(final Mail mail) {
        mockedMail = mail;
        mockedSMTPSession = new BaseFakeSMTPSession() {

            private HashMap state = new HashMap();

            private String ipAddress = "192.168.0.1";

            private String host = "localhost";

            private boolean relayingAllowed;

            public String getRemoteHost() {
                return host;
            }

            public String getRemoteIPAddress() {
                return ipAddress;
            }

            public Map getState() {
                state.put(SMTPSession.SENDER, "sender@james.apache.org");
                return state;
            }

            public boolean isRelayingAllowed() {
                return relayingAllowed;
            }

            public void setRelayingAllowed(boolean relayingAllowed) {
                this.relayingAllowed = relayingAllowed;
            }
        };

        return mockedSMTPSession;

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
        HookResult response = handler.onMessage(session, mockedMail);

        assertEquals("Email was not rejected", response.getResult(), HookReturnCode.DECLINED);
        assertEquals("email was not spam", mockedMail.getAttribute(
                SpamAssassinInvoker.FLAG_MAIL_ATTRIBUTE_NAME), "NO");
        assertNotNull("spam hits", mockedMail.getAttribute(
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
        HookResult response = handler.onMessage(session, mockedMail);

        assertEquals("Email was not rejected", response.getResult(), HookReturnCode.DECLINED);
        assertEquals("email was spam", mockedMail.getAttribute(
                SpamAssassinInvoker.FLAG_MAIL_ATTRIBUTE_NAME), "YES");
        assertNotNull("spam hits", mockedMail.getAttribute(
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
        HookResult response = handler.onMessage(session, mockedMail);

        assertEquals("Email was rejected", response.getResult(), HookReturnCode.DENY);
        assertEquals("email was spam", mockedMail.getAttribute(
                SpamAssassinInvoker.FLAG_MAIL_ATTRIBUTE_NAME), "YES");
        assertNotNull("spam hits", mockedMail.getAttribute(
                SpamAssassinInvoker.STATUS_MAIL_ATTRIBUTE_NAME));
    }

}
