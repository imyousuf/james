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


package org.apache.james.smtpserver.core;

import org.apache.james.Constants;
import org.apache.james.smtpserver.ConnectHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.mailet.dates.RFC822DateFormat;

import java.util.Date;

/**
 * This ConnectHandler can be used to activate pop-before-smtp
 */
public class WelcomeMessageHandler implements ConnectHandler {

    /**
     * SMTP Server identification string used in SMTP headers
     */
    private final static String SOFTWARE_TYPE = "JAMES SMTP Server "
                                                 + Constants.SOFTWARE_VERSION;

    /**
     * Static RFC822DateFormat used to generate date headers
     */
    private final static RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    /**
     * @see org.apache.james.smtpserver.ConnectHandler#onConnect(SMTPSession)
     */
    public void onConnect(SMTPSession session) {
        String smtpGreeting = session.getConfigurationData().getSMTPGreeting();

        SMTPResponse welcomeResponse;
        // if no greeting was configured use a default
        if (smtpGreeting == null) {
            // Initially greet the connector
            // Format is:  Sat, 24 Jan 1998 13:16:09 -0500
            welcomeResponse = new SMTPResponse(SMTPRetCode.SERVICE_READY,
                          new StringBuffer(256)
                          .append(session.getConfigurationData().getHelloName())
                          .append(" SMTP Server (")
                          .append(SOFTWARE_TYPE)
                          .append(") ready ")
                          .append(rfc822DateFormat.format(new Date())));
        } else {
            welcomeResponse = new SMTPResponse(SMTPRetCode.SERVICE_READY,smtpGreeting);
        }
        session.writeSMTPResponse(welcomeResponse);
    }

}
