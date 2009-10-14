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

import org.apache.commons.logging.Log;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * Filter which check if valid Object are used
 */
public class ResponseValidationFilter extends AbstractValidationFilter {


    public ResponseValidationFilter(Log logger) {
        super(logger);
    }

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#messageSent(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, org.apache.mina.core.write.WriteRequest)
     */
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        if (writeRequest.getMessage() instanceof SMTPResponse) {
            super.messageReceived(nextFilter, session, writeRequest);
        } else {
            // TODO check what to do when we receive an invalid object.
            getLogger().error("The Sent object is not an instance of SMTPResponse");
          
        }
    }
}

