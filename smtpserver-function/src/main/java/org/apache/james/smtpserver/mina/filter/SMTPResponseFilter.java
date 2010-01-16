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

import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.socket.mina.filter.AbstractResponseFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;


/**
 * Filter to convert SMTPResponse to String Objects
 * 
 */
public class SMTPResponseFilter extends AbstractResponseFilter {
   
    private static final String SCHEDULE_CLOSE_ATTRIBUTE = SMTPResponseFilter.class.getName() + ".closeAttribute";

    public final static String NAME = "smtpResponseFilter";

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#filterWrite(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, org.apache.mina.core.write.WriteRequest)
     */
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {

        if (writeRequest.getMessage() instanceof SMTPResponse) {
            SMTPResponse response = (SMTPResponse) writeRequest.getMessage();
            if (response != null) {
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
                    session.setAttribute(getCloseAttribute());
                }
            }
        } else {
            super.filterWrite(nextFilter, session, writeRequest);
        }

    }

	@Override
	protected String getCloseAttribute() {
		return SCHEDULE_CLOSE_ATTRIBUTE;
	}

}
