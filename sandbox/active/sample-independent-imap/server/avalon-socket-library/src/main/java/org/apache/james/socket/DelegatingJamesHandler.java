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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

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
        remoteHost = null;
        remoteIP = null;
    }

    /**
     * @see org.apache.james.socket.AbstractJamesHandler#errorHandler(java.lang.RuntimeException)
     */
    protected void errorHandler(RuntimeException e) {
       protocolHandler.errorHandler(e);
    }
    
    /**
     * @see org.apache.james.socket.ProtocolHandlerHelper#defaultErrorHandler(java.lang.RuntimeException)
     */
    public void defaultErrorHandler(RuntimeException e) {
        super.errorHandler(e);
    }
    
    /**
     * @see org.apache.james.socket.ProtocolHandlerHelper#getRemoteIP()
     */
    public String getRemoteIP() {
        return remoteIP;
    }

    /**
     * @see org.apache.james.socket.ProtocolHandlerHelper#getInputReader()
     */
    public CRLFTerminatedReader getInputReader() {
        return inReader;
    }

    /**
     * @see org.apache.james.socket.ProtocolHandlerHelper#getInputStream()
     */
    public InputStream getInputStream() {
        return in;
    }

    /**
     * @see org.apache.james.socket.ProtocolHandlerHelper#getOutputStream()
     */
    public OutputStream getOutputStream() {
        return outs;
    }

    /**
     * @see org.apache.james.socket.ProtocolHandlerHelper#getOutputWriter()
     */
    public PrintWriter getOutputWriter() {
        return out;
    }

    /**
     * @see org.apache.james.socket.ProtocolHandlerHelper#getRemoteHost()
     */
    public String getRemoteHost() {
        return remoteHost;
    }
    
    /**
     * @see org.apache.james.socket.ProtocolHandlerHelper#getAvalonLogger()
     */
    public Logger getAvalonLogger() {
        return getLogger();
    }

    /**
     * @see org.apache.james.socket.ProtocolHandlerHelper#getWatchdog()
     */
    public Watchdog getWatchdog() {
        return theWatchdog;
    }

		/**
		 * @see org.apache.james.socket.ProtocolHandlerHelper#getSocket()
		 */
		public Socket getSocket() {
			return socket;
		}

}
