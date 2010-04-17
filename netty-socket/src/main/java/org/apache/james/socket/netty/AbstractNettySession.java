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

package org.apache.james.socket.netty;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLEngine;

import org.apache.commons.logging.Log;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.TLSSupportedSession;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedStream;

/**
 * Abstract implementation of TLSSupportedSession which use Netty
 * 
 * 
 */
public abstract class AbstractNettySession implements TLSSupportedSession {

    protected ChannelHandlerContext handlerContext;
    protected InetSocketAddress socketAddress;
    protected Log logger;
    protected SSLEngine engine;
    protected String user;

    public AbstractNettySession(Log logger, ChannelHandlerContext handlerContext, SSLEngine engine) {
        this.handlerContext = handlerContext;
        this.socketAddress = (InetSocketAddress) handlerContext.getChannel().getRemoteAddress();
        this.logger = logger;
        this.engine = engine;
    }

    public AbstractNettySession(Log logger, ChannelHandlerContext handlerContext) {
        this(logger, handlerContext, null);
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
    public ChannelHandlerContext getChannelHandlerContext() {
        return handlerContext;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#isStartTLSSupported()
     */
    public boolean isStartTLSSupported() {
        return engine != null;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#isTLSStarted()
     */
    public boolean isTLSStarted() {
        
        if (isStartTLSSupported()) {
            return getChannelHandlerContext().getPipeline().get("sslHandler") != null;
        }
        
        return false;
    }

    /**
     * @see org.apache.james.api.protocol.TLSSupportedSession#startTLS()
     */
    public void startTLS() throws IOException {
        if (isStartTLSSupported() && isTLSStarted() == false) {
            getChannelHandlerContext().getChannel().setReadable(false);
            SslHandler filter = new SslHandler(engine);
            filter.getEngine().setUseClientMode(false);
            resetState();
            getChannelHandlerContext().getPipeline().addFirst("sslHandler", filter);
            getChannelHandlerContext().getChannel().setReadable(true);
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
        Channel channel = getChannelHandlerContext().getChannel();
        if (response != null && channel.isConnected()) {
            channel.write(response);
            if (response.isEndSession()) {
                channel.close();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#writeStream(java.io.InputStream)
     */
    public void writeStream(InputStream stream) {
        Channel channel = getChannelHandlerContext().getChannel();
        if (stream != null && channel.isConnected()) {
            channel.write(new ChunkedStream(stream));
        }
    }
    
    

}
