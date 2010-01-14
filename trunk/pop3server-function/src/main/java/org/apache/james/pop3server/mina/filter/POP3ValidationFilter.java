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

import org.apache.commons.logging.Log;
import org.apache.james.pop3server.POP3Request;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.socket.mina.filter.AbstractValidationFilter;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

/**
 * Validation filter which checks if the written objects are valid for POP3
 * 
 */
public class POP3ValidationFilter extends AbstractValidationFilter {

    public POP3ValidationFilter(Log logger) {
        super(logger);
    }

    @Override
    protected WriteRequest errorRequest(Object obj) {
        return new DefaultWriteRequest(POP3Response.ERR_RESPONSE + " Cannot handle message of type " + (obj != null ? obj.getClass() : "NULL"));
    }

    @Override
    protected WriteRequest errorResponse(Object obj) {
        return null;
    }

    @Override
    protected boolean isValidRequest(Object requestObject) {
        if (requestObject instanceof POP3Request) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean isValidResponse(Object responseObject) {
        if (responseObject instanceof POP3Response) {
            return true;
        }
        return false;
    }

}
