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

package org.apache.james.remotemanager;

import java.util.Map;

import org.apache.james.socket.LogEnabledSession;
import org.apache.james.socket.Watchdog;

public interface RemoteManagerSession extends LogEnabledSession{

    public final static String CURRENT_USERREPOSITORY= "CURRENT_USERREPOSITORY";
    public final static String HEADER_IDENTIFIER = "header=";
    public final static String REGEX_IDENTIFIER = "regex=";
    public final static String KEY_IDENTIFIER = "key=";
    

    /**
     * Return state map which should get used to store temporary data 
     * 
     * @return stateMap
     */
    public Map<Object,Object> getState();
    
    /**
     * Write response to client
     * 
     * @param response
     */
    public void writeRemoteManagerResponse(RemoteManagerResponse response);
	
    
    public Watchdog getWatchdog();
}
