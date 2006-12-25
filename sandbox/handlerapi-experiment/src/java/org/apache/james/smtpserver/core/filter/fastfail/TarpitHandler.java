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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;

/**
 * Add tarpit support to SMTPServer. See http://www.palomine.net/qmail/tarpit.html for more information
 *
 */
public class TarpitHandler extends AbstractLogEnabled implements
        CommandHandler, Configurable {

    private int tarpitRcptCount = 0;

    private long tarpitSleepTime = 5000;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration)
            throws ConfigurationException {

        Configuration configTarpitRcptCount = handlerConfiguration.getChild(
                "tarpitRcptCount", false);
        if (configTarpitRcptCount != null) {
            setTarpitRcptCount(configTarpitRcptCount.getValueAsInteger(0));
        }

        if (tarpitRcptCount == 0)
            throw new ConfigurationException(
                    "Please set the tarpitRcptCount bigger values as 0");

        Configuration configTarpitSleepTime = handlerConfiguration.getChild(
                "tarpitSleepTime", false);
        if (configTarpitSleepTime != null) {
            setTarpitSleepTime(configTarpitSleepTime.getValueAsLong(5000));
        }

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
     * Add a sleep for the given milliseconds
     * 
     * @param timeInMillis
     *            Time in ms
     * @throws InterruptedException
     * 
     * TODO: Remember to add an sleep method to our IO implementation
     */
    private void sleep(float timeInMillis) throws InterruptedException {
        Thread.sleep((long) timeInMillis);
    }

    /**
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(org.apache.james.smtpserver.SMTPSession, java.lang.String, java.lang.String) 
     */  
    public SMTPResponse onCommand(SMTPSession session, String command, String parameters) {

        int rcptCount = 0;
        rcptCount = session.getRcptCount();
        rcptCount++;

        if (rcptCount > tarpitRcptCount) {
            try {
                sleep(tarpitSleepTime);
            } catch (InterruptedException e) {
            // ignore
            }
        }
        
        return null;
    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("RCPT");
        
        return implCommands;
    }
}
