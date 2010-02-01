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

import java.util.Date;

import org.apache.james.api.protocol.ConnectHandler;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.mailet.base.RFC822DateFormat;

/**
 * This ConnectHandler print the greeting on connecting
 */
public class WelcomeMessageHandler implements ConnectHandler<SMTPSession> {

    /**
     * Static RFC822DateFormat used to generate date headers
     */
    private final static RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    /**
     * @see org.apache.james.smtpserver.protocol.ConnectHandler#onConnect(SMTPSession)
     */
    public void onConnect(SMTPSession session) {
        String smtpGreeting = session.getSMTPGreeting();

        SMTPResponse welcomeResponse;
        // if no greeting was configured use a default
        if (smtpGreeting == null) {
            // Initially greet the connector
            // Format is:  Sat, 24 Jan 1998 13:16:09 -0500
            welcomeResponse = new SMTPResponse(SMTPRetCode.SERVICE_READY,
                          new StringBuilder(256)
                          .append(session.getHelloName())
                          .append(" SMTP Server (")
                          .append(getProductName())
                          .append(") ready ")
                          .append(rfc822DateFormat.format(new Date())));
        } else {
            welcomeResponse = new SMTPResponse(SMTPRetCode.SERVICE_READY,smtpGreeting);
        }
        session.writeResponse(welcomeResponse);
    }
    
    protected String getProductName() {
        return "JAMES SMTP Server";
    }

}
