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
package org.apache.james.smtpserver.netty;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.LifecycleUtil;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.impl.AbstractChannelUpstreamHandler;
import org.apache.james.protocols.smtp.SMTPConfiguration;
import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPRetCode;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.smtpserver.SMTPConstants;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

/**
 * {@link ChannelUpstreamHandler} which is used by the SMTPServer
 *
 */
public class SMTPChannelUpstreamHandler extends AbstractChannelUpstreamHandler{
    private final Log logger;
    private final SMTPConfiguration conf;
    private final SSLContext context;
    private String[] enabledCipherSuites;

    public SMTPChannelUpstreamHandler(ProtocolHandlerChain chain,
            SMTPConfiguration conf, Log logger) {
        this(chain, conf, logger, null, null);
    }
    
    public SMTPChannelUpstreamHandler(ProtocolHandlerChain chain,
            SMTPConfiguration conf, Log logger, SSLContext context, String[] enabledCipherSuites) {
        super(chain);
        this.conf = conf;
        this.logger = logger;
        this.context = context;
        this.enabledCipherSuites = enabledCipherSuites;
    }
    
    @Override
    protected ProtocolSession createSession(ChannelHandlerContext ctx) throws Exception {
        if (context != null) {
            SSLEngine engine = context.createSSLEngine();
            if (enabledCipherSuites != null && enabledCipherSuites.length > 0) {
                engine.setEnabledCipherSuites(enabledCipherSuites);
            }
            return new SMTPNettySession(conf, logger, ctx, engine);
        } else {
            return  new SMTPNettySession(conf, logger, ctx);
        }
    }

    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {        
        Channel channel = ctx.getChannel();
        if (e.getCause() instanceof TooLongFrameException) {
            ctx.getChannel().write(new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_COMMAND_UNRECOGNIZED, "Line length exceeded. See RFC 2821 #4.5.3.1."));
        } else {
            if (channel.isConnected()) {
                ctx.getChannel().write(new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unable to process request"));
            }
            logger.debug("Unable to process request", e.getCause());
            cleanup(channel);
            channel.close();
        }
       
        super.exceptionCaught(ctx, e);
    }

    /**
     * Cleanup temporary files 
     * 
     * @param channel
     */
    protected void cleanup(Channel channel) {
        // Make sure we dispose everything on exit on session close
        SMTPSession smtpSession = (SMTPSession) attributes.get(channel);
        
        if (smtpSession != null) {
            LifecycleUtil.dispose(smtpSession.getState().get(SMTPConstants.MAIL));
            LifecycleUtil.dispose(smtpSession.getState().get(SMTPConstants.DATA_MIMEMESSAGE_STREAMSOURCE));
        }
        
        super.cleanup(channel);
    }
    
}
