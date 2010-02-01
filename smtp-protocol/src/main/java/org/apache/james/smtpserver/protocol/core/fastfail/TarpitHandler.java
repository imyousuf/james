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


import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.james.smtpserver.protocol.hook.RcptHook;
import org.apache.mailet.MailAddress;

/**
 * Add tarpit support to SMTPServer. See http://www.palomine.net/qmail/tarpit.html for more information
 *
 */
public class TarpitHandler implements RcptHook {

    private int tarpitRcptCount = 0;

    private long tarpitSleepTime = 5000;




    /**
     * Set the tarpit count after which the tarpit sleep time will be activated
     * 
     * @param tarpitRcptCount
     */
    public void setTarpitRcptCount(int tarpitRcptCount) {
        this.tarpitRcptCount = tarpitRcptCount;
    }

    /**
     * Set the tarpit sleep time
     * 
     * @param tarpitSleepTime
     *            Time in milliseconds
     */
    public void setTarpitSleepTime(long tarpitSleepTime) {
        this.tarpitSleepTime = tarpitSleepTime;
    }

    /**
     * @see org.apache.james.smtpserver.protocol.hook.RcptHook#doRcpt(org.apache.james.smtpserver.protocol.SMTPSession, org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
     */
    public HookResult doRcpt(SMTPSession session, MailAddress sender, MailAddress rcpt) {

        int rcptCount = 0;
        rcptCount = session.getRcptCount();
        rcptCount++;

        if (rcptCount > tarpitRcptCount) {
            session.sleep(tarpitSleepTime);
        }
        
        return new HookResult(HookReturnCode.DECLINED);
    }
}
