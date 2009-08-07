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
import org.apache.james.smtpserver.core.filter.fastfail.TarpitHandler;
import org.apache.james.test.mock.avalon.MockLogger;

import junit.framework.TestCase;

public class TarpitHandlerTest extends TestCase {

    private SMTPSession session;

    private SMTPSession setupMockedSession(final int rcptCount) {
        session = new AbstractSMTPSession() {

            public int getRcptCount() {
                return rcptCount;
            }

        };

        return session;
    }

    public void testTarpit() {
        long tarpitTime = 1000;
        long tarpitTolerance = 100;
        long startTime;
        TarpitHandler handler = new TarpitHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());

        handler.setTarpitRcptCount(2);
        handler.setTarpitSleepTime(tarpitTime);

        // no tarpit used
        startTime = System.currentTimeMillis();
        handler.onRcpt(setupMockedSession(0),null);
        assertTrue("No tarpit",
                (System.currentTimeMillis() - startTime) < tarpitTime - tarpitTolerance);

        // tarpit used
        startTime = System.currentTimeMillis();
        handler.onRcpt(setupMockedSession(0),null);
        assertTrue("tarpit",
                (System.currentTimeMillis() - startTime) >= tarpitTime - tarpitTolerance);
    }
}
