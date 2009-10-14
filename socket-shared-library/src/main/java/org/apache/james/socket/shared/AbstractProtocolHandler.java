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



package org.apache.james.socket.shared;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.james.socket.api.ProtocolContext;
import org.apache.james.socket.api.ProtocolHandler;

/**
 * Abstract base class for ProtocolHandler
 * 
 *
 */
public abstract class AbstractProtocolHandler implements ProtocolHandler, TLSSupportedSession{

    private ProtocolContext context;
    private String user;

    /**
     * @see org.apache.james.socket.api.ProtocolHandler#resetHandler()
     */
    public void resetHandler() {
        user = null;
        resetHandlerInternal();
    }

    /**
     * @see org.apache.james.socket.api.ProtocolHandler#handleProtocol(org.apache.james.socket.api.ProtocolContext)
     */
    public void handleProtocol(ProtocolContext context) throws IOException {
        this.context = context;
        handleProtocolInternal(context);
    }

    /**
     * @see org.apache.james.socket.shared.TLSSupportedSession#getRemoteHost()
     */
    public String getRemoteHost() {
        return context.getRemoteHost();
    }

    /**
     * @see org.apache.james.socket.shared.TLSSupportedSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return context.getRemoteIP();
    }

    /**
     * @see org.apache.james.socket.shared.TLSSupportedSession#getUser()
     */
    public String getUser() {
        return user;
    }

    /**
     * @see org.apache.james.socket.shared.TLSSupportedSession#isTLSStarted()
     */
    public boolean isTLSStarted() {
        return context.isSecure();
    }

    /**
     * @see org.apache.james.socket.shared.TLSSupportedSession#setUser(java.lang.String)
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @see org.apache.james.socket.shared.TLSSupportedSession#startTLS()
     */
    public void startTLS() throws IOException {
        context.secure();
    }
    
    
    /**
     * @see org.apache.james.socket.shared.TLSSupportedSession#getLogger()
     */
    public Log getLogger() {
        return context.getLogger();
    }

    /**
     * @see org.apache.james.socket.api.ProtocolHandler#fatalFailure(java.lang.RuntimeException, org.apache.james.socket.api.ProtocolContext)
     */
    public void fatalFailure(RuntimeException e, ProtocolContext context) {
    }
    
    /**
     * @see #handleProtocol(ProtocolContext)
     */
    protected abstract void handleProtocolInternal(ProtocolContext context) throws IOException;
    
    /**
     * @see #resetHandler()
     */
    protected abstract void resetHandlerInternal();

}
