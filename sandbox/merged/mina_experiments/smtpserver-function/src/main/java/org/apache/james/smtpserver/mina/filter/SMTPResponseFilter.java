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

import java.util.Locale;

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
public class SMTPResponseFilter extends IoFilterAdapter {
   
    private static final String SCHEDULE_CLOSE_ATTRIBUTE = SMTPRequestFilter.class.getName() + ".closeAttribute";

    /**
     * (non-Javadoc)
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#messageReceived(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, java.lang.Object)
     */
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        if (message instanceof String) {
            String cmdString = (String) message;
            if (cmdString != null) {
                cmdString = cmdString.trim();
            }

            String curCommandArgument = null;
            String curCommandName = null;
            int spaceIndex = cmdString.indexOf(" ");
            if (spaceIndex > 0) {
                curCommandName = cmdString.substring(0, spaceIndex);
                curCommandArgument = cmdString.substring(spaceIndex + 1);
            } else {
                curCommandName = cmdString;
            }
            curCommandName = curCommandName.toUpperCase(Locale.US);

            nextFilter.messageReceived(session, new SMTPRequest(curCommandName,
                    curCommandArgument));
        } else {
            super.messageReceived(nextFilter, session, message);
        }
    }

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#messageSent(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, org.apache.mina.core.write.WriteRequest)
     */
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        super.messageSent(nextFilter, session, writeRequest);
        // System.err.println("### "+arg2);
        if (session.containsAttribute(SCHEDULE_CLOSE_ATTRIBUTE)) {
            // Close the session if no more scheduled writes are there.
            if (session.getScheduledWriteMessages() == 0) {
                session.close(true);
            }
        }
    }

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#filterWrite(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, org.apache.mina.core.write.WriteRequest)
     */
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {

        if (writeRequest.getMessage() instanceof SMTPResponse) {
            SMTPResponse response = (SMTPResponse) writeRequest.getMessage();
            if (response != null) {
                // Iterator i = esmtpextensions.iterator();
                for (int k = 0; k < response.getLines().size(); k++) {
                    StringBuffer respBuff = new StringBuffer(256);
                    respBuff.append(response.getRetCode());
                    if (k == response.getLines().size() - 1) {
                        respBuff.append(" ");
                        respBuff.append(response.getLines().get(k));
                        nextFilter.filterWrite(session,
                                new DefaultWriteRequest(respBuff.toString()));
                    } else {
                        respBuff.append("-");
                        respBuff.append(response.getLines().get(k));
                        nextFilter.filterWrite(session,
                                new DefaultWriteRequest(respBuff.toString()));
                    }
                }

                if (response.isEndSession()) {
                    session.setAttribute(SCHEDULE_CLOSE_ATTRIBUTE);
                    // arg0.filterClose(arg1);
                    // arg1.close();
                }
            }
        } else {
            super.filterWrite(nextFilter, session, writeRequest);
        }

    }

}
