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

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.util.watchdog.Watchdog;

import java.io.IOException;
import java.io.InputStream;

/**
 * Common Handler code
 */
public class DelegatingJamesHandler extends AbstractJamesHandler implements ProtocolHandlerHelper {

    protected ProtocolHandler protocolHandler;
    
    
    public DelegatingJamesHandler(ProtocolHandler delegated) {
        this.protocolHandler = delegated;
        this.protocolHandler.setProtocolHandlerHelper(this);
    }
    
    /**
     * This method will be implemented checking for the correct class
     * type.
     * 
     * @param theData Configuration Bean.
     */
    public void setConfigurationData(Object theData) {
        protocolHandler.setConfigurationData(theData);
    }
    
    /**
     * Handle the protocol
     * 
     * @throws IOException get thrown if an IO error is detected
     */
    public void handleProtocol() throws IOException {
        protocolHandler.handleProtocol();
    }

   /**
    * Resets the handler data to a basic state.
    */
    public void resetHandler() {
        protocolHandler.resetHandler();
    }
    
    /**
     * @see org.apache.james.socket.ProtocolHandlerHelper#getRemoteIP()
     */
    public String getRemoteIP() {
        return remoteIP;
    }

    public CRLFTerminatedReader getInputReader() {
        return inReader;
    }

    public InputStream getInputStream() {
        return in;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public Watchdog getWatchdog() {
        return getWatchdog();
    }

    public void setRemoteHost(String host) {
        remoteHost = host;
    }

    public void setRemoteIP(String ip) {
        remoteIP = ip;
    }
    
    public Logger getAvalonLogger() {
        return getLogger();
    }

}
