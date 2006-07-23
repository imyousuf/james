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

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.smtpserver.core.RoaminUsersHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.util.RoaminUsersHelper;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.mailet.Mail;

import junit.framework.TestCase;

public class RoaminUsersHandlerTest extends TestCase {

    private SMTPSession mockedSession;

    private void setupMockedSMTPSession() {
        mockedSession = new SMTPSession() {
            private boolean relayingAllowed = false;

            public void abortMessage() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
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
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public int getRcptCount() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteHost() {
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

            public String getRemoteIPAddress() {
                return "192.168.200.1";
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
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
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
                throw new UnsupportedOperationException(
                        "Unimplemented mock service");
            }

        };
    }

    public void testAuthWorks() {

        RoaminUsersHandler handler = new RoaminUsersHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        setupMockedSMTPSession();
        RoaminUsersHelper.addIPAddress("192.168.200.1");

        assertFalse(mockedSession.isRelayingAllowed());
        handler.onConnect(mockedSession);
        assertTrue(mockedSession.isRelayingAllowed());
    }

    public void testIPGetRemoved() {
        long sleepTime = 100;
        RoaminUsersHandler handler = new RoaminUsersHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        setupMockedSMTPSession();
        RoaminUsersHelper.addIPAddress("192.168.200.1");
        assertFalse(mockedSession.isRelayingAllowed());

        try {
            Thread.sleep(sleepTime);
            RoaminUsersHelper.removeExpiredIP(10);
            handler.onConnect(mockedSession);
            assertFalse(mockedSession.isRelayingAllowed());

        } catch (InterruptedException e) {
            //ignore
        }
    }
}
