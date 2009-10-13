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
package org.apache.james.smtpserver.mina;

import org.apache.commons.logging.Log;
import org.apache.james.smtpserver.SMTPRequest;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPRetCode;
import org.apache.james.socket.shared.LogEnabled;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

public class AbstractValidationFilter extends IoFilterAdapter implements LogEnabled {

    private Log logger;
    
    public void setLog(Log logger) {
        this.logger = logger;
    }
    
    protected Log getLogger() {
        return logger;
    }

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#filterWrite(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, org.apache.mina.core.write.WriteRequest)
     */
    public void filterWrite(NextFilter arg0, IoSession arg1, WriteRequest arg2)
            throws Exception {
        Object obj = arg2.getMessage();

        if (obj instanceof SMTPResponse || obj instanceof SMTPRequest) {
            super.filterWrite(arg0, arg1, arg2);
        } else {
            logger.error("WriteRequest holds not a SMTPResponseImpl but "
                    + (obj == null ? "NULL" : obj.getClass()));
            arg0.filterWrite(arg1, new DefaultWriteRequest(new SMTPResponse(
                    SMTPRetCode.TRANSACTION_FAILED, "Cannot handle Request")));
        }
    }


}
