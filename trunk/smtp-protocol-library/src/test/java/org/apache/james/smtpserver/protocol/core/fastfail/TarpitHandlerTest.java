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
package org.apache.james.smtpserver.protocol.core.fastfail;

import javax.mail.internet.ParseException;

import junit.framework.TestCase;

import org.apache.james.smtpserver.protocol.BaseFakeSMTPSession;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.mailet.MailAddress;

public class TarpitHandlerTest extends TestCase {

    private SMTPSession session;

    private SMTPSession setupMockedSession(final int rcptCount) {
        session = new BaseFakeSMTPSession() {

            public int getRcptCount() {
                return rcptCount;
            }
            
            public void sleep(long ms) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        };

        return session;
    }

    public void testTarpit() throws ParseException {
        long tarpitTime = 1000;
        long tarpitTolerance = 100;
        long startTime;
        TarpitHandler handler = new TarpitHandler();
        
        handler.setTarpitRcptCount(2);
        handler.setTarpitSleepTime(tarpitTime);

        // no tarpit used
        startTime = System.currentTimeMillis();
        handler.doRcpt(setupMockedSession(0),null,new MailAddress("test@test"));
        assertTrue("No tarpit",
                (System.currentTimeMillis() - startTime) < tarpitTime - tarpitTolerance);

        // tarpit used
        startTime = System.currentTimeMillis();
        handler.doRcpt(setupMockedSession(3),null,new MailAddress("test@test"));
        assertTrue("tarpit",
                (System.currentTimeMillis() - startTime) >= tarpitTime - tarpitTolerance);
    }
}
