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

import java.util.List;

/**
 * Protocol response to send to the client
 * 
 *
 */
public interface Response {

    
    /**
     * Append line to response
     * 
     * @param line 
     */
    public void appendLine(CharSequence line);
   
    /**
     * Return a List of all response lines stored in this Response
     * 
     * @return all responseLines
     */
    public List<CharSequence> getLines();

    /**
     * Return the raw representation of the stored Response
     * 
     * @return rawLine the raw Response
     */
    public String getRawLine();

    /**
     * Return true if the session is ended
     * 
     * @return true if session is ended
     */
    public boolean isEndSession();
    /**
     * Set to true to end the session
     * 
     * @param endSession
     */
    public void setEndSession(boolean endSession);

}
