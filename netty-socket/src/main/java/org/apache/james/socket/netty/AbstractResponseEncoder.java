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

import static org.jboss.netty.buffer.ChannelBuffers.*;

import java.util.List;


import org.apache.james.protocols.api.Response;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * {@link OneToOneEncoder} implementation which handles the encoding of {@link Response} implementations
 * 
 *
 * @param <R>
 */
@ChannelPipelineCoverage("all")
public abstract class AbstractResponseEncoder<R extends Response> extends OneToOneEncoder{

    private Class<? extends Response> classType;
    private String charset;

    public AbstractResponseEncoder(Class< ? extends Response> classType, String charset) {
        this.classType = classType;
        this.charset = charset;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected Object encode(ChannelHandlerContext arg0, Channel arg1, Object obj) throws Exception {
        if (classType.isInstance(obj)) {
            StringBuilder builder = new StringBuilder();
            R response = (R) obj;
            List<String> lines = getResponse(response);
            for (int i = 0; i < lines.size(); i++) {
                builder.append(lines.get(i));
                if (i < lines.size()) {
                    builder.append("\r\n");
                }
            }
            return copiedBuffer(builder.toString(), charset);
        }
        return obj;
    }

    /**
     * Return a list which contains the response
     */
    protected abstract List<String> getResponse(R response);
}
