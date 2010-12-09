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
package org.apache.james.smtpserver.log;

import org.apache.james.protocols.api.CommandHandler;
import org.apache.james.protocols.api.CommandHandlerResultHandler;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPSession;

/**
 * Log every 5xx and 4xx response in INFO level. The rest is logged via DEBUG level
 * 
 * TODO: This should go to protocols
 * 
 *
 */
public class CommandHandlerResultLogger implements CommandHandlerResultHandler<SMTPResponse, SMTPSession> {

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.CommandHandlerResultHandler#onResponse(org.apache.james.protocols.api.ProtocolSession, org.apache.james.protocols.api.Response, long, org.apache.james.protocols.api.CommandHandler)
     */
    public Response onResponse(ProtocolSession session, SMTPResponse response, long executionTime, CommandHandler<SMTPSession> handler) {
        String code = response.getRetCode();
        String msg = handler.getClass().getName() + ": " + response.toString();
        
        // check if the response was a perm error or a temp error
        if (code.startsWith("5") || code.startsWith("4")) {
            session.getLogger().info(msg);
        } else {
            session.getLogger().debug(msg);
        }
        return response;
    }

}
