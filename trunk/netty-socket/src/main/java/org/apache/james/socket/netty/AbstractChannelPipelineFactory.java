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

import static org.jboss.netty.channel.Channels.*;


import org.apache.james.protocols.api.Response;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

/**
 * Abstract base class for {@link ChannelPipelineFactory} implementations
 * 
 *
 */
public abstract class AbstractChannelPipelineFactory implements ChannelPipelineFactory{

    private final Timer timer = new HashedWheelTimer();
    /*
     * (non-Javadoc)
     * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
     */
    public ChannelPipeline getPipeline() throws Exception {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = pipeline();
        
        pipeline.addLast("connectionLimit", new ConnectionLimitUpstreamHandler(getMaxConnections()));

        pipeline.addLast("connectionPerIpLimit", new ConnectionPerIpLimitUpstreamHandler(getMaxConnectionsPerIP()));

        
        // Add the text line codec combination first,
        // decoder
        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
       
        // encoder
        pipeline.addLast("encoderResponse", createEncoder());

        pipeline.addLast("streamer", new ChunkedWriteHandler());
        pipeline.addLast("timeoutHandler", new TimeoutHandler(timer, 120,120,0));
        pipeline.addLast("coreHandler", createHandler());


        return pipeline;
    }

    /**
     * Create the core {@link ChannelUpstreamHandler} to use
     * 
     * @return coreHandeler
     */
    protected abstract ChannelUpstreamHandler createHandler();
    
    /**
     * Create the {@link Response} Encoder
     * 
     * @return encoder
     */
    protected abstract OneToOneEncoder createEncoder();
    
    /**
     * Return the timeout in seconds
     * 
     * @return timeout
     */
    protected abstract int getTimeout();


    /**
     * Return the max connections 
     * 
     * @return max connections
     */
    protected abstract int getMaxConnections();
    
    /**
     * Return the max connections per ip
     * 
     * @return max connections per ip
     */
    protected abstract int getMaxConnectionsPerIP();


}
