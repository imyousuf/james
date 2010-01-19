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

package org.apache.james.api.protocol;

import java.util.Map;

import org.apache.commons.logging.Log;

/**
 * Session for a protcol. Every new connection generates a new session
 * 
 *
 */
public interface ProtocolSession {
   
    /**
     * Gets the context sensitive log for this session.
     * @return log, not null
     */
    public Log getLogger();
    
    
    /**
     * Return Map which can be used to store objects within a session
     * 
     * @return state
     */
    public Map<String, Object> getState();
    
    /**
     * Reset the state
     */
    public void resetState();
    
    /**
     * Write the response back to the client
     * 
     * @param response
     */
    public void writeResponse(Response response);
    
    /**
     * Returns host name of the client
     *
     * @return hostname of the client
     */
    public String getRemoteHost();

    /**
     * Returns host ip address of the client
     *
     * @return host ip address of the client
     */
    public String getRemoteIPAddress();
}
