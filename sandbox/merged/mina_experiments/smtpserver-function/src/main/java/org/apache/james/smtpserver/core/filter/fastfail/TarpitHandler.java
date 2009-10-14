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



package org.apache.james.smtpserver.core.filter.fastfail;


import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.RcptHook;
import org.apache.james.socket.configuration.Configurable;
import org.apache.mailet.MailAddress;

/**
 * Add tarpit support to SMTPServer. See http://www.palomine.net/qmail/tarpit.html for more information
 *
 */
public class TarpitHandler implements RcptHook, Configurable {

    private int tarpitRcptCount = 0;

    private long tarpitSleepTime = 5000;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration)
            throws ConfigurationException {
        setTarpitRcptCount(handlerConfiguration.getInt("tarpitRcptCount", 0));

        if (tarpitRcptCount == 0)
            throw new ConfigurationException(
                    "Please set the tarpitRcptCount bigger values as 0");

        setTarpitSleepTime(handlerConfiguration.getLong("tarpitSleepTime", 5000));


        if (tarpitSleepTime == 0)
            throw new ConfigurationException(
                    "Please set the tarpitSleepTimeto a bigger values as 0");

    }

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
     * @see org.apache.james.smtpserver.hook.RcptHook#doRcpt(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
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
