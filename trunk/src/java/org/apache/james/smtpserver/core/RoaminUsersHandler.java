/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
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
package org.apache.james.smtpserver.core;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.smtpserver.ConnectHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.RoaminUsersHelper;

/**
 * This ConnectHandler can be used to activate pop-before-smtp
 */
public class RoaminUsersHandler implements ConnectHandler, Configurable {

    /**
     * The time after which ipAddresses should be handled as expired
     */
    private long expireTime;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration config = arg0.getChild("expireTime", false);

        if (config != null) {
            setExpireTime(config.getValueAsLong(RoaminUsersHelper.EXPIRE_TIME));
        }
    }

    /**
     * Set the time after which an ipAddresses should be handled as expired
     * 
     * @param expireTime The time in ms
     */
    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * @see org.apache.james.smtpserver.ConnectHandler#onConnect(SMTPSession)
     */
    public void onConnect(SMTPSession session) {

        // some kind of random cleanup process
        if (Math.random() > 0.5) {
            RoaminUsersHelper.removeExpiredIP(expireTime);
        }

        // Check if the ip is allowed to relay
        if (RoaminUsersHelper.isAuthorized(session.getRemoteIPAddress())) {
            session.setRelayingAllowed(true);
        }
    }

}
