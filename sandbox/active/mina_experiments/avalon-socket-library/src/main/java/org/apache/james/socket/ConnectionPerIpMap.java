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




package org.apache.james.socket;

import java.util.HashMap;

/**
 * Map which holds information about the connection count per ip
 */
public class ConnectionPerIpMap {
    
    /**
     * A HashMap of client ip and connections
     */
    private final HashMap<String, Integer> connectionPerIP = new HashMap<String, Integer>();
    
    /**
     * Raise the connectionCount for the given ipAdrress
     * 
     * @param ip raise the connectionCount for the given ipAddress
     */
    public synchronized void addConnectionPerIP (String ip) {
        connectionPerIP.put(ip,new Integer(getConnectionPerIP(ip) +1));
    }
    
    /**
     * Get the count of connections for the given ip
     * 
     * @param ip the ipAddress to get the connections for. 
     * @return count
     */
    public synchronized int getConnectionPerIP(String ip) {
        Integer value = (Integer) connectionPerIP.get(ip);
        if (value == null) {
            return 0;
        } 
        return value.intValue();
    }
    
    /**
     * Set the connection count for the given ipAddress
     * 
     * @param ip ipAddres for which we want to set the count
     */
    public synchronized void removeConnectionPerIP (String ip) {

        int count = getConnectionPerIP(ip);
        if (count > 1) {
            connectionPerIP.put(ip,new Integer(count -1));
        } else {
            // not need this entry any more
            connectionPerIP.remove(ip);
        }

    }
    
    /**
     * Clear the connection count map
     */
    public synchronized void clearConnectionPerIP () {
        connectionPerIP.clear();
    }
    
}
