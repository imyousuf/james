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
package org.apache.james.server;

/**
 * JMX MBean interface for servers
 */
public interface ServerMBean {
	
	/**
	 * Return the maximum allowed concurrent connections for the server
	 *  
	 * @return maxConcurrentConnections
	 */
	public int getMaximumConcurrentConnections();
	
	/**
	 * Return the current connection count
	 * 
	 * @return currentConnection
	 */
	public int getCurrentConnections();
	
	/**
	 * Return true if the server is enabled
	 * 
	 * @return isEnabled
	 */
    public boolean isEnabled();

    /**
     * Return true if startTLS is supported by the server
     * 
     * @return startTLS
     */
	public boolean getStartTLSSupported();


    /**
     * Return the IPAddress to which the server is bound
     *  
     * @return ipAddress or null if not bound to some specifc ip
     */
    public String getIPAddress();
    
    /**
     * Return the port number to which ther server is bound
     * @return
     */
    public int  getPort();
    
    /**
     * Return the socket type of the server. Which can either be plain or secure
     * 
     */  
    public String getSocketType();
    
    /**
     * Return the service type of the server
     * 
     */  
    public String getServiceType();
    
    /**
     * Return true if the server is started, which basicly means it is bound to a address and accept connections
     * 
     * @return started
     */
    public boolean isStarted();
    
    /**
     * Start the server
     * 
     * @return start
     */
    public boolean start();
    
    /**
     * Stop the server
     * 
     * @return stop
     */
    public boolean stop();
}
