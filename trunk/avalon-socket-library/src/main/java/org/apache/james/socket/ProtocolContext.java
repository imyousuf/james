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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.commons.logging.Log;

/**
 * This is the helper interface provided to ProtocolHandlers to let them
 * communicate with the outside world.
 */
public interface ProtocolContext {

    /**
     * Writes a response to the client and flush it.
     * @param responseString the response string
     */
    public void writeLoggedFlushedResponse(String responseString);
    
    /**
     * Writes a response to the client without flushing.
     * @param responseString the response string
     */
    public void writeLoggedResponse(String responseString);
    
    /**
     * The watchdog is used to deal with timeouts.
     * @return the watchdog instance
     */
    public Watchdog getWatchdog();

    /**
     * getter for the remote hostname
     * @return remote hostname 
     */
    public String getRemoteHost();
    
    /**
     * getter for the remote ip
     * @return remote ip 
     */
    public String getRemoteIP();
    
    /**
     * Returns a CRLF terminated line reader
     * @return line reader
     */
    public CRLFTerminatedReader getInputReader();
    
    /**
     * Returns the raw input stream
     * @return the raw inputstream
     */
    public InputStream getInputStream();
    
    /**
     * Returns the raw outputstream
     * @return outputstream
     */
    public OutputStream getOutputStream();
    
    /**
     * Returns the printwriter.
     * @return the output printwriter
     */
    public PrintWriter getOutputWriter();
    
    /**
     * Provides basic errorhandling cleanup.
     * @param e the runtimeexception
     */
    public void defaultErrorHandler(RuntimeException e);
    
    /**
     * Is the socket disconnected?
     * @return true if the connection has disconnected,
     * false otherwise
     */
    public boolean isDisconnected();
    
    /**
     * Gets a context sensitive logger.
     * @return not null
     */
    public Log getLogger();
}