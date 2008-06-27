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

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.smtpserver.core.POP3BeforeSMTPHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.util.POP3BeforeSMTPHelper;

import junit.framework.TestCase;

public class POP3BeforeSMTPHandlerTest extends TestCase {

    private SMTPSession mockedSession;

    private void setupMockedSMTPSession() {
    mockedSession = new AbstractSMTPSession() {
        private boolean relayingAllowed = false;

        public String getRemoteIPAddress() {
        return "192.168.200.1";
        }

        public boolean isRelayingAllowed() {
        return relayingAllowed;
        }

        public void setRelayingAllowed(boolean relayingAllowed) {
        this.relayingAllowed = relayingAllowed;
        }
        
        public void doChain() {
        
        }

    };
    }

    public void testAuthWorks() {

    POP3BeforeSMTPHandler handler = new POP3BeforeSMTPHandler();

    ContainerUtil.enableLogging(handler, new MockLogger());
    setupMockedSMTPSession();
    POP3BeforeSMTPHelper.addIPAddress("192.168.200.1");

    assertFalse(mockedSession.isRelayingAllowed());
    handler.onConnect(mockedSession);
    assertTrue(mockedSession.isRelayingAllowed());
    }

    public void testIPGetRemoved() {
    long sleepTime = 100;
    POP3BeforeSMTPHandler handler = new POP3BeforeSMTPHandler();

    ContainerUtil.enableLogging(handler, new MockLogger());
    setupMockedSMTPSession();
    POP3BeforeSMTPHelper.addIPAddress("192.168.200.1");
    assertFalse(mockedSession.isRelayingAllowed());

    try {
        Thread.sleep(sleepTime);
        POP3BeforeSMTPHelper.removeExpiredIP(10);
        handler.onConnect(mockedSession);
        assertFalse(mockedSession.isRelayingAllowed());

    } catch (InterruptedException e) {
        //ignore
    }
    }

    public void testThrowExceptionOnIllegalExpireTime() {
    boolean exception = false;
    POP3BeforeSMTPHandler handler = new POP3BeforeSMTPHandler();

    ContainerUtil.enableLogging(handler, new MockLogger());
    setupMockedSMTPSession();

    try {
        handler.setExpireTime("1 unit");
    } catch (NumberFormatException e) {
        exception = true;
    }
    assertTrue(exception);
    }

    public void testValidExpireTime() {
    boolean exception = false;
    POP3BeforeSMTPHandler handler = new POP3BeforeSMTPHandler();

    ContainerUtil.enableLogging(handler, new MockLogger());
    setupMockedSMTPSession();

    try {
        handler.setExpireTime("1 hour");
    } catch (NumberFormatException e) {
        exception = true;
    }
    assertFalse(exception);
    }

}
