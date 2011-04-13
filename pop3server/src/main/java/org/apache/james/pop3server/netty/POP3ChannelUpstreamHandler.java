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

import org.apache.james.pop3server.POP3HandlerConfigurationData;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.impl.AbstractChannelUpstreamHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.slf4j.Logger;

/**
 * {@link ChannelUpstreamHandler} which is used for the POP3 Server
 */
public class POP3ChannelUpstreamHandler extends AbstractChannelUpstreamHandler {

    private final Logger logger;
    private final POP3HandlerConfigurationData conf;
    private final SSLContext context;
    private String[] enabledCipherSuites;

    public POP3ChannelUpstreamHandler(ProtocolHandlerChain chain, POP3HandlerConfigurationData conf, Logger logger, SSLContext context, String[] enabledCipherSuites) {
        super(chain);
        this.logger = logger;
        this.conf = conf;
        this.context = context;
        this.enabledCipherSuites = enabledCipherSuites;
    }

    public POP3ChannelUpstreamHandler(ProtocolHandlerChain chain, POP3HandlerConfigurationData conf, Logger logger) {
        this(chain, conf, logger, null, null);
    }

    @Override
    protected ProtocolSession createSession(ChannelHandlerContext ctx) throws Exception {
        if (context != null) {
            SSLEngine engine = context.createSSLEngine();
            if (enabledCipherSuites != null && enabledCipherSuites.length > 0) {
                engine.setEnabledCipherSuites(enabledCipherSuites);
            }
            return new POP3NettySession(conf, logger, ctx, engine);
        } else {
            return new POP3NettySession(conf, logger, ctx);
        }
    }


}
