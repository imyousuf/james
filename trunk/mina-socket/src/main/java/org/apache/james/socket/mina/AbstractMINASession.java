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

package org.apache.james.socket.mina;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.TLSSupportedSession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;

/**
 * Abstract implementation of TLSSupportedSession which use IoSession
 * 
 * 
 */
public abstract class AbstractMINASession implements TLSSupportedSession {

    protected IoSession session;
    protected InetSocketAddress socketAddress;
    protected Log logger;
    protected SSLContext context;
    protected String user;

    public AbstractMINASession(Log logger, IoSession session, SSLContext context) {
        this.session = session;
        this.socketAddress = (InetSocketAddress) session.getRemoteAddress();
        this.logger = logger;
        this.context = context;
    }

    public AbstractMINASession(Log logger, IoSession session) {
        this(logger, session, null);
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#getRemoteHost()
     */
    public String getRemoteHost() {
        return socketAddress.getHostName();
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return socketAddress.getAddress().getHostAddress();
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#getUser()
     */
    public String getUser() {
        return user;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#setUser(java.lang.String)
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Return underlying IoSession
     * 
     * @return session
     */
    public IoSession getIoSession() {
        return session;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#isStartTLSSupported()
     */
    public boolean isStartTLSSupported() {
        return context != null;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#isTLSStarted()
     */
    public boolean isTLSStarted() {
        if (isStartTLSSupported()) {
            return session.getFilterChain().contains("sslFilter");
        }
        return false;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#startTLS()
     */
    public void startTLS() throws IOException {
        if (isStartTLSSupported()) {
            session.suspendRead();
            SslFilter filter = new SslFilter(context);
            resetState();
            session.getFilterChain().addFirst("sslFilter", filter);
            session.resumeRead();
        }
    }

    /**
     * @see org.apache.james.api.protocol.ProtocolSession#getLogger()
     */
    public Log getLogger() {
        return logger;
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.ProtocolSession#writeResponse(org.apache.james.api.protocol.Response)
     */
    public void writeResponse(Response response) {
        if (getIoSession().isConnected()) {
            getIoSession().write(response);
        }
    }

}
