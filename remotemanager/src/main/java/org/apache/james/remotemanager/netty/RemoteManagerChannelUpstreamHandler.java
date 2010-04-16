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

import org.apache.commons.logging.Log;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.james.socket.netty.AbstractChannelUpstreamHandler;
import org.jboss.netty.channel.ChannelHandlerContext;

public class RemoteManagerChannelUpstreamHandler extends AbstractChannelUpstreamHandler{

    private Log logger;
    private RemoteManagerHandlerConfigurationData config;
    public RemoteManagerChannelUpstreamHandler(RemoteManagerHandlerConfigurationData config, ProtocolHandlerChain chain, Log logger) {
        super(chain);
        this.logger = logger;
        this.config = config;
    }

    @Override
    protected ProtocolSession createSession(ChannelHandlerContext ctx) throws Exception {
        RemoteManagerSession rSession  = new NettyRemoteManagerSession(config, logger, ctx.getChannel());
        rSession.getState().put(RemoteManagerSession.CURRENT_USERREPOSITORY, "LocalUsers");
        return rSession;
    }

}
