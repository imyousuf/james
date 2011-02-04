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
package org.apache.james.imapserver.netty;

import static org.jboss.netty.channel.Channels.pipeline;

import javax.annotation.Resource;
import javax.net.ssl.SSLEngine;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.protocols.impl.ChannelGroupHandler;
import org.apache.james.protocols.impl.TimeoutHandler;
import org.apache.james.protocols.lib.netty.AbstractConfigurableAsyncServer;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.connection.ConnectionLimitUpstreamHandler;
import org.jboss.netty.handler.connection.ConnectionPerIpLimitUpstreamHandler;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.util.HashedWheelTimer;

/**
 * NIO IMAP Server which use Netty
 *
 */
public class IMAPServer extends AbstractConfigurableAsyncServer implements ImapConstants, IMAPServerMBean, NettyConstants {

    private static final String softwaretype = "JAMES "+VERSION+" Server ";
    
    private String hello;
    private ImapProcessor processor;
    private ImapEncoder encoder;

    private ImapDecoder decoder;

    private boolean compress;

    private int maxLineLength;

    private int inMemorySizeLimit;
    
    // Use a big default
    public final static int DEFAULT_MAX_LINE_LENGTH = 65536;
    
    // Use 10MB as default
    public final static int DEFAULT_IN_MEMORY_SIZE_LIMIT = 10485760;

    @Resource(name="imapDecoder")
    public void setImapDecoder(ImapDecoder decoder) {
        this.decoder = decoder;
    }
    
    @Resource(name="imapEncoder")
    public void setImapEncoder(ImapEncoder encoder) {
        this.encoder = encoder;
    }
    
    @Resource(name="imapProcessor")
    public void setImapProcessor(ImapProcessor processor) {
        this.processor = processor;
    }
    
    @Override
    public void doConfigure( final HierarchicalConfiguration configuration ) throws ConfigurationException {
        super.doConfigure(configuration);
        hello  = softwaretype + " Server " + getHelloName() + " is ready.";
        compress = configuration.getBoolean("compress", false);
        maxLineLength = configuration.getInt("maxLineLength", DEFAULT_MAX_LINE_LENGTH);
        inMemorySizeLimit = configuration.getInt("inMemorySizeLimit", DEFAULT_IN_MEMORY_SIZE_LIMIT);
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.mina.AbstractAsyncServer#getDefaultPort()
     */
    public int getDefaultPort() {
        return 143;
    }

 
    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.mina.AbstractAsyncServer#getServiceType()
     */
    public String getServiceType() {
        return "IMAP Service";
    }

    @Override
    protected ChannelPipelineFactory createPipelineFactory(final ChannelGroup group) {
        return new ChannelPipelineFactory() {
            private final ChannelGroupHandler groupHandler = new ChannelGroupHandler(group);
            private final HashedWheelTimer timer = new HashedWheelTimer();
            
            // Timeout of 30 minutes See rfc2060 5.4 for details
            private final static int TIMEOUT = 30 * 60;

            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = pipeline();
                pipeline.addLast(GROUP_HANDLER, groupHandler);
                pipeline.addLast(TIMEOUT_HANDLER, new TimeoutHandler(timer, TIMEOUT));
                pipeline.addLast(CONNECTION_LIMIT_HANDLER, new ConnectionLimitUpstreamHandler(IMAPServer.this.connectionLimit));

                pipeline.addLast(CONNECTION_LIMIT_PER_IP_HANDLER, new ConnectionPerIpLimitUpstreamHandler(IMAPServer.this.connPerIP));


                
                // Add the text line decoder which limit the max line length, don't strip the delimiter and use CRLF as delimiter
                pipeline.addLast(FRAMER, new DelimiterBasedFrameDecoder(maxLineLength, false, Delimiters.lineDelimiter()));
                pipeline.addLast(REQUEST_DECODER, new ImapRequestFrameDecoder(decoder, inMemorySizeLimit));

                
                if (isSSLSocket()) {
                    // We need to set clientMode to false.
                    // See https://issues.apache.org/jira/browse/JAMES-1025
                    SSLEngine engine = getSSLContext().createSSLEngine();
                    engine.setUseClientMode(false);
                    pipeline.addFirst(SSL_HANDLER, new SslHandler(engine));
                    
                }
                pipeline.addLast(CONNECTION_COUNT_HANDLER, getConnectionCountHandler());
                
                
                if (isStartTLSSupported())  {
                    pipeline.addLast(CORE_HANDLER,  new ImapChannelUpstreamHandler(hello, processor, encoder, getLogger(), compress, getSSLContext(), getEnabledCipherSuites()));
                } else {
                    pipeline.addLast(CORE_HANDLER,  new ImapChannelUpstreamHandler(hello, processor, encoder, getLogger(), compress));
                }
                
                return pipeline;
            }
           
        };
    }

    @Override
    protected String getDefaultJMXName() {
        return "imapserver";
    }


}
