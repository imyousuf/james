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
package org.apache.james.smtpserver.netty;

import javax.net.ssl.SSLEngine;

import org.apache.james.protocols.smtp.SMTPConfiguration;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.smtpserver.netty.SMTPServer.SMTPHandlerConfigurationDataImpl;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;

/**
 * {@link SMTPSession} implementation for use with Netty
 */
public class SMTPNettySession extends org.apache.james.protocols.smtp.netty.SMTPNettySession {
   
    private SMTPConfiguration theConfigData;


    public SMTPNettySession(SMTPConfiguration theConfigData, Logger logger, Channel channel, SSLEngine engine) {
        super(theConfigData, logger, channel, engine);
        this.theConfigData = theConfigData;
    }


    public SMTPNettySession(SMTPConfiguration theConfigData, Logger logger, Channel channel) {
        super(theConfigData, logger, channel);
        this.theConfigData = theConfigData;
    }


    public boolean verifyIdentity() {
        if (theConfigData instanceof SMTPHandlerConfigurationDataImpl) {
            return ((SMTPHandlerConfigurationDataImpl) theConfigData).verifyIdentity();
        } else {
            return true;
        }
    }
}
