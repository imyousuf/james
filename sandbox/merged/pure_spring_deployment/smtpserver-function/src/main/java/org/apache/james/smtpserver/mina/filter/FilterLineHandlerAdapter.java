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

import org.apache.james.smtpserver.protocol.LineHandler;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;


/**
 * Adapter class which call the wrapped LineHandler on MessageReceived callback
 * 
 */
public final class FilterLineHandlerAdapter extends IoFilterAdapter {

    public final static String SMTP_SESSION = "SMTP_SESSION";

    private LineHandler lineHandler;

    public FilterLineHandlerAdapter(LineHandler lineHandler) {
        this.lineHandler = lineHandler;
    }

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#messageReceived(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, java.lang.Object)
     */
    public void messageReceived(NextFilter arg0, IoSession session, Object arg2)
            throws Exception {
        lineHandler.onLine((SMTPSession) session.getAttribute(SMTP_SESSION),
                (((String) arg2) + "\r\n").getBytes());
    }
}
