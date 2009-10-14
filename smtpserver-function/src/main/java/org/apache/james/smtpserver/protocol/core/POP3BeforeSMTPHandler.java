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


package org.apache.james.smtpserver.protocol.core;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.smtpserver.protocol.ConnectHandler;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.util.POP3BeforeSMTPHelper;
import org.apache.james.util.TimeConverter;

/**
 * This ConnectHandler can be used to activate pop-before-smtp
 */
public class POP3BeforeSMTPHandler implements ConnectHandler {

    /**
     * The time after which ipAddresses should be handled as expired
     */
    private long expireTime = POP3BeforeSMTPHelper.EXPIRE_TIME;


    /**
     * @see org.apache.james.socket.configuration.Configurable#configure(org.apache.commons.configuration.Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException {
        try {
            setExpireTime(config.getString("expireTime",null));
        } catch (NumberFormatException e) {
            throw new ConfigurationException(
                    "Please configure a valid expireTime: "
                            + e.getMessage());
        }
    }

    /**
     * Set the time after which an ipAddresses should be handled as expired
     * 
     * @param rawExpireTime
     *            The time
     */
    public void setExpireTime(String rawExpireTime) {
        if (rawExpireTime != null) {
            this.expireTime = TimeConverter.getMilliSeconds(rawExpireTime);
        }
    }

    /**
     * @see org.apache.james.smtpserver.protocol.ConnectHandler#onConnect(SMTPSession)
     */
    public void onConnect(SMTPSession session) {

        // some kind of random cleanup process
        if (Math.random() > 0.99) {
            POP3BeforeSMTPHelper.removeExpiredIP(expireTime);
        }

        // Check if the ip is allowed to relay
        if (!session.isRelayingAllowed()
                && POP3BeforeSMTPHelper.isAuthorized(session.getRemoteIPAddress())) {
            session.setRelayingAllowed(true);
        }
    }

}
