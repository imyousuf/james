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
package org.apache.james.protocols.lib.jmx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.CommandHandler;
import org.apache.james.protocols.api.handler.ProtocolHandler;
import org.apache.james.protocols.api.handler.ProtocolHandlerResultHandler;
import org.apache.james.protocols.api.handler.ExtensibleHandler;
import org.apache.james.protocols.api.handler.LifecycleAwareProtocolHandler;
import org.apache.james.protocols.api.handler.WiringException;

/**
 * Expose JMX statistics for {@link CommandHandler}
 */
public abstract class AbstractCommandHandlerResultJMXMonitor<R extends Response, S extends ProtocolSession> implements ProtocolHandlerResultHandler<R, S>, ExtensibleHandler, LifecycleAwareProtocolHandler {

    private Map<String, AbstractCommandHandlerStats<R>> cStats = new HashMap<String, AbstractCommandHandlerStats<R>>();
    private String jmxName;

    /**
     * @see
     * org.apache.james.protocols.api.handler.ProtocolHandlerResultHandler#onResponse(
     * org.apache.james.protocols.api.ProtocolSession,
     * org.apache.james.protocols.api.Response, long,
     * org.apache.james.protocols.api.handler.ProtocolHandler)
     */
    public Response onResponse(ProtocolSession session, R response, long executionTime, ProtocolHandler handler) {
        if (handler instanceof CommandHandler) {
            String name = handler.getClass().getName();
            AbstractCommandHandlerStats<R> stats = cStats.get(name);
            if (stats != null) {
                stats.increment(response);
            }
        }
        return response;
    }

    /**
     * @see
     * org.apache.james.protocols.api.handler.ExtensibleHandler#getMarkerInterfaces()
     */
    public List<Class<?>> getMarkerInterfaces() {
        List<Class<?>> marker = new ArrayList<Class<?>>();
        marker.add(CommandHandler.class);
        return marker;
    }

    /**
     * @see
     * org.apache.james.protocols.api.handler.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void wireExtensions(Class<?> interfaceName, List<?> extension) throws WiringException {
        if (interfaceName.equals(CommandHandler.class)) {
            // add stats for all hooks
            for (int i = 0; i < extension.size(); i++) {
                CommandHandler c = (CommandHandler) extension.get(i);
                if (equals(c) == false) {
                    String cName = c.getClass().getName();
                    try {
                        cStats.put(cName, createCommandHandlerStats(c));
                    } catch (Exception e) {
                        throw new WiringException("Unable to wire Hooks", e);
                    }
                }
            }
        }
    }

    /**
     * Create the {@link AbstractCommandHandlerStats} for the given
     * {@link CommandHandler}
     * 
     * @param handler
     * @return stats
     * @throws Exception
     */
    protected abstract AbstractCommandHandlerStats<R> createCommandHandlerStats(CommandHandler<S> handler) throws Exception;


    @Override
    public void init(Configuration config) throws ConfigurationException {
        this.jmxName = config.getString("jmxName", getDefaultJMXName());
        
    }
    protected abstract String getDefaultJMXName();
    
    protected String getJMXName() {
        return jmxName;
    }
    
    @Override
    public void destroy() {
        Iterator<AbstractCommandHandlerStats<R>> it = cStats.values().iterator();
        while(it.hasNext()) {
            it.next().dispose();
        }
    }

}
