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
import org.apache.james.smtpserver.protocol.SMTPRequest;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.socket.mina.filter.AbstractValidationFilter;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

public class SMTPValidationFilter extends AbstractValidationFilter{


    public SMTPValidationFilter(Log logger) {
        super(logger);
    }

    /**
     * @see org.apache.james.socket.mina.filter.AbstractValidationFilter#errorResponse(java.lang.Object)
     */
    protected WriteRequest errorResponse(Object obj) {
       return null;
    }

    /**
     * @see org.apache.james.socket.mina.filter.AbstractValidationFilter#errorRequest(java.lang.Object)
     */
    protected WriteRequest errorRequest(Object obj) {
        return new DefaultWriteRequest(new SMTPResponse(
                SMTPRetCode.TRANSACTION_FAILED,
                "Cannot handle message of type " + (obj != null ? obj.getClass() : "NULL")));
    }

    /**
     * @see org.apache.james.socket.mina.filter.AbstractValidationFilter#isValidRequest(java.lang.Object)
     */
    protected boolean isValidRequest(Object requestObject) {
        if (requestObject instanceof SMTPRequest) {
            return true;
        }
        return false;
    }

    /**
     * (non-Javadoc)
     * @see org.apache.james.socket.mina.filter.AbstractValidationFilter#isValidResponse(java.lang.Object)
     */
    protected boolean isValidResponse(Object responseObject) {
        if (responseObject instanceof SMTPResponse) {
            return true;
        }
        return false;
    }

}
