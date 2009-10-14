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
package org.apache.james.smtpserver.mina.filter;

import org.apache.james.smtpserver.SMTPRequest;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

/**
 * Filter to convert SMTPResponse to String Objects
 * 
 */
public class SMTPRequestFilter extends IoFilterAdapter {

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#messageReceived(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, java.lang.Object)
     */
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message)
            throws Exception {
        if (message instanceof String) {
            SMTPResponse resp = (SMTPResponse) session
                    .getAttribute("CURRENT_RESPONSE");
            if (resp == null) {
                resp = new SMTPResponse();
                resp.setRetCode(((String) message).substring(0, 3));
                session.setAttribute("CURRENT_RESPONSE", resp);
            }
            resp.appendLine(((String) message).substring(4));
            if (!((String) message).substring(3, 4).equals("-")) {
                nextFilter.messageReceived(session, resp);
                session.removeAttribute("CURRENT_RESPONSE");
            }
        } else {
            super.messageReceived(nextFilter, session, message);
        }
    }

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#filterWrite(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, org.apache.mina.core.write.WriteRequest)
     */
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {

        if (writeRequest.getMessage() instanceof SMTPRequest) {
            SMTPRequest req = (SMTPRequest) writeRequest.getMessage();
            StringBuffer line = new StringBuffer();
            line.append(req.getCommand());
            if (req.getArgument() != null && req.getArgument().length() > 0) {
                line.append(" ");
                line.append(req.getArgument());
            }
            nextFilter.filterWrite(session, new DefaultWriteRequest(line.toString()));
        } else {
            nextFilter.filterWrite(session, writeRequest);
        }

    }

    
 
}
