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

import javax.net.ssl.SSLContext;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.ssl.SslHandler;

/**
 * Abstract base class for {@link ChannelPipeline} implementations which use TLS 
 * 
 *
 */
public abstract class AbstractSSLAwareChannelPipelineFactory extends AbstractChannelPipelineFactory{

    
    
    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline =  super.getPipeline();

        if (isSSLSocket()) {
            pipeline.addFirst("sslHandler", new SslHandler(getSSLContext().createSSLEngine()));
        }
        return pipeline;
    }

    /**
     * Return if the socket is using SSL/TLS
     * 
     * @return isSSL
     */
    protected abstract boolean isSSLSocket();
    
    /**
     * Return the SSL context
     * 
     * @return context
     */
    protected abstract SSLContext getSSLContext();
}
