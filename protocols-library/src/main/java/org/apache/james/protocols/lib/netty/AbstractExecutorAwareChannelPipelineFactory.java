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
package org.apache.james.protocols.lib.netty;

import org.apache.james.protocols.impl.AbstractSSLAwareChannelPipelineFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.execution.ExecutionHandler;

/**
 * Abstract base class which should get used if you MAY need an {@link ExecutionHandler}
 * 
 *
 */
public abstract class AbstractExecutorAwareChannelPipelineFactory extends AbstractSSLAwareChannelPipelineFactory{


    public AbstractExecutorAwareChannelPipelineFactory(int timeout, int maxConnections, int maxConnectsPerIp, ChannelGroup group) {
        super(timeout, maxConnections, maxConnectsPerIp, group);
    }
    public AbstractExecutorAwareChannelPipelineFactory(int timeout, int maxConnections, int maxConnectsPerIp, ChannelGroup group, String[] enabledCipherSuites) {
        super(timeout, maxConnections, maxConnectsPerIp, group, enabledCipherSuites);
    }
    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeLine = super.getPipeline();
        pipeLine.addBefore("coreHandler", "countHandler", getConnectionCountHandler());
        ExecutionHandler ehandler = getExecutionHandler();
        if (ehandler != null) {
            pipeLine.addBefore("coreHandler", "executionHandler", ehandler);
        }
        return pipeLine;
    }

    /**
     * Return the {@link ExecutionHandler} to use or null if none
     * should get used
     * 
     * @return eHandler
     */
    protected abstract ExecutionHandler getExecutionHandler();
    
    /**
     * REturn the {@link ConnectionCountHandler} to use
     * 
     * @return cHandler
     */
    protected abstract ConnectionCountHandler getConnectionCountHandler();
}
