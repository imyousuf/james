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

package org.apache.james.pop3server.mina.filter;

import java.util.Locale;

import org.apache.james.pop3server.POP3Request;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.socket.mina.filter.AbstractResponseFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

public class POP3ResponseFilter extends AbstractResponseFilter {

    private static final String SCHEDULE_CLOSE_ATTRIBUTE = POP3ResponseFilter.class.getName() + ".closeAttribute";

    @Override
    protected String getCloseAttribute() {
        return SCHEDULE_CLOSE_ATTRIBUTE;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#messageReceived(org.apache.mina.core.filterchain.IoFilter.NextFilter,
     *      org.apache.mina.core.session.IoSession, java.lang.Object)
     */
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
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

            nextFilter.messageReceived(session, new POP3Request(curCommandName, curCommandArgument));
        } else {
            super.messageReceived(nextFilter, session, message);
        }
    }

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#filterWrite(org.apache.mina.core.filterchain.IoFilter.NextFilter,
     *      org.apache.mina.core.session.IoSession,
     *      org.apache.mina.core.write.WriteRequest)
     */
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {

        if (writeRequest.getMessage() instanceof POP3Response) {
            POP3Response response = (POP3Response) writeRequest.getMessage();
            if (response != null) {
                for (int k = 0; k < response.getLines().size(); k++) {
                    StringBuffer respBuff = new StringBuffer(256);
                    if (k == 0) {
                        respBuff.append(response.getRetCode());
                        respBuff.append(" ");
                        respBuff.append(response.getLines().get(k));

                    } else {
                        respBuff.append(response.getLines().get(k));
                    }
                    nextFilter.filterWrite(session, new DefaultWriteRequest(respBuff.toString()));
                }

                if (response.isEndSession()) {
                    session.setAttribute(getCloseAttribute());
                }
            }
        } else {
            super.filterWrite(nextFilter, session, writeRequest);
        }

    }
}
