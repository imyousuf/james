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


package org.apache.james.pop3server.core;

import org.apache.james.Constants;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.ConnectHandler;

public class WelcomeMessageHandler implements ConnectHandler<POP3Session>{
    /** POP3 Server identification string used in POP3 headers */
    private static final String softwaretype        = "JAMES POP3 Server "
                                                        + Constants.SOFTWARE_VERSION;

    /**
     * @see org.apache.james.pop3server.ConnectHandler#onConnect(org.apache.james.pop3server.POP3Session)
     */
    public void onConnect(POP3Session session) {
        StringBuilder responseBuffer = new StringBuilder();

        // Initially greet the connector
        // Format is:  Sat, 24 Jan 1998 13:16:09 -0500
        responseBuffer.append(session.getConfigurationData().getHelloName())
                    .append(" POP3 server (")
                    .append(softwaretype)
                    .append(") ready ");
        POP3Response response = new POP3Response(POP3Response.OK_RESPONSE, responseBuffer.toString());
        session.writeResponse(response);
    }

}
