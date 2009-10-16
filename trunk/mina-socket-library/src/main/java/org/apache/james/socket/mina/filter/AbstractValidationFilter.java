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
package org.apache.james.socket.mina.filter;

import org.apache.commons.logging.Log;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

public abstract class AbstractValidationFilter extends IoFilterAdapter {

    private Log logger;
    
    public AbstractValidationFilter(Log logger) {
        this.logger = logger;
    }
    
    protected Log getLogger() {
        return logger;
    }

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#messageReceived(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, java.lang.Object)
     */
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (isValidRequest(message)) {
            super.messageReceived(nextFilter, session, message);
        } else {
            errorRequest(message);
        }
    }

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#filterWrite(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, org.apache.mina.core.write.WriteRequest)
     */
    public void filterWrite(NextFilter arg0, IoSession arg1, WriteRequest arg2)
            throws Exception {
        Object obj = arg2.getMessage();

        if (isValidRequest(obj) || isValidResponse(obj)) {
            super.filterWrite(arg0, arg1, arg2);
        } else {
            logger.error("WriteRequest holds not a an valid Object but "
                    + (obj == null ? "NULL" : obj.getClass()));
            WriteRequest request = errorResponse(obj);
            if (request != null) {
                arg0.filterWrite(arg1, request); 
            }
        }
    }

    /**
     * Check if the given Object is valid as Request. If so return true
     * 
     * @param requestObject
     * @return isValidRequest
     */
    protected abstract boolean isValidRequest(Object requestObject);
    
    
    /**
     * Check if the given Object is a valid Response. If so return true
     * 
     * @param responseObject
     * @return isValidResponse
     */
    protected abstract boolean isValidResponse(Object responseObject);
    
  
    /**
     * Return the WriteRequest which should get written to the client. If you don't want to write anything just return
     * null
     * 
     * @param obj the original obj
     * @return writeRequest
     */
    protected abstract WriteRequest errorResponse(Object obj);
    
    
    /**
     * Return the WriteRequest which should get written to the client. If you don't want to write anything just return
     * null
     * 
     * @param obj the original obj
     * @return writeRequest
     */
    protected abstract WriteRequest errorRequest(Object obj);

}
