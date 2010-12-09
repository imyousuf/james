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

package org.apache.james.remotemanager.netty;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.james.protocols.api.LineHandler;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.impl.LineHandlerUpstreamHandler;
import org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.stream.ChunkedStream;

/**
 * {@link RemoteManagerSession} implementation for Netty
 *
 */
public class NettyRemoteManagerSession implements RemoteManagerSession {
    private Log logger;
    private Channel channel;
    private Map<String, Object> state = new HashMap<String, Object>();
    private RemoteManagerHandlerConfigurationData config;
    private int lineHandlerCount = 0;
    private InetSocketAddress socketAddress;
    private String id;
    private static Random random = new Random();

    
    public NettyRemoteManagerSession(RemoteManagerHandlerConfigurationData config, Log logger, Channel channel) {
        this.logger = logger;
        this.channel = channel;
        this.config = config;
        this.socketAddress = (InetSocketAddress) channel.getRemoteAddress();
        this.id = random.nextInt(1024) + "";

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.remotemanager.RemoteManagerSession#getState()
     */
    public Map<String, Object> getState() {
        return state;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.protocol.LogEnabledSession#getLogger()
     */
    public Log getLogger() {
        return logger;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.protocol.LogEnabledSession#resetState()
     */
    public void resetState() {
        state.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.apache.james.remotemanager.RemoteManagerSession#
     * getAdministrativeAccountData()
     */
    public Map<String, String> getAdministrativeAccountData() {
        return config.getAdministrativeAccountData();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerSession#popLineHandler()
     */
    public void popLineHandler() {
        channel.getPipeline().remove("lineHandler" + lineHandlerCount);
        lineHandlerCount--;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerSession#pushLineHandler(org.apache.james.remotemanager.LineHandler)
     */
    public void pushLineHandler(LineHandler<RemoteManagerSession> overrideCommandHandler) {
        lineHandlerCount++;
        channel.getPipeline().addBefore("coreHandler", "lineHandler" + lineHandlerCount, new LineHandlerUpstreamHandler<RemoteManagerSession>(overrideCommandHandler));
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#getRemoteHost()
     */
    public String getRemoteHost() {
        return socketAddress.getHostName();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return socketAddress.getAddress().getHostAddress();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#writeResponse(org.apache.james.api.protocol.Response)
     */
    public void writeResponse(Response response) {
        if (channel.isConnected()) {
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
        if (channel.isConnected()) {
            channel.write(new ChunkedStream(stream));    
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ProtocolSession#getSessionID()
     */
    public String getSessionID() {
        return id;
    }
}
