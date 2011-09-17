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

package org.apache.james.pop3server.netty;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.ProtocolSessionFactory;
import org.apache.james.protocols.impl.BasicChannelUpstreamHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;

public class POP3ChannelUpstreamHandler extends BasicChannelUpstreamHandler{

    private boolean zeroCopy;

    public POP3ChannelUpstreamHandler(ProtocolHandlerChain chain, ProtocolSessionFactory sessionFactory, Logger logger, boolean zeroCopy) {
        super(chain, sessionFactory, logger);
        this.zeroCopy = zeroCopy;
    }

    public POP3ChannelUpstreamHandler(ProtocolHandlerChain chain, ProtocolSessionFactory sessionFactory, Logger logger, SSLContext context, String[] enabledCipherSuites, boolean zeroCopy) {
        super(chain, sessionFactory, logger, context, enabledCipherSuites);
        this.zeroCopy = zeroCopy;
    }

    @Override
    protected ProtocolSession createSession(ChannelHandlerContext ctx) throws Exception {
        SSLEngine engine = null;
        if (context != null) {
            engine = context.createSSLEngine();
            if (enabledCipherSuites != null && enabledCipherSuites.length > 0) {
                engine.setEnabledCipherSuites(enabledCipherSuites);
            }
        }
        
        return sessionFactory.newSession(new POP3ProtocolTransport(ctx.getChannel(), engine, zeroCopy));
    }


}
