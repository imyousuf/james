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
import org.apache.james.protocols.api.WiringException;
import org.apache.james.protocols.api.handler.ExtensibleHandler;
import org.apache.james.protocols.api.handler.LifecycleAwareProtocolHandler;
import org.apache.james.protocols.api.handler.LineHandler;
import org.apache.james.protocols.api.handler.LineHandlerResultHandler;

/**
 * Handler which will gather statistics for {@link LineHandler}'s
 * 
 * @param <S>
 */
public abstract class AbstractLineHandlerResultJMXMonitor<S extends ProtocolSession> implements LineHandlerResultHandler<S>, ExtensibleHandler, LifecycleAwareProtocolHandler {

    private Map<String, LineHandlerStats> lStats = new HashMap<String, LineHandlerStats>();
    private String jmxName;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.protocols.api.LineHandlerResultHandler#onResponse(org
     * .apache.james.protocols.api.ProtocolSession, boolean, long,
     * org.apache.james.protocols.api.LineHandler)
     */
    public boolean onResponse(ProtocolSession session, boolean response, long executionTime, LineHandler<S> handler) {
        lStats.get(handler.getClass().getName()).increment(response);
        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.protocols.api.ExtensibleHandler#getMarkerInterfaces()
     */
    public List<Class<?>> getMarkerInterfaces() {
        List<Class<?>> marker = new ArrayList<Class<?>>();
        marker.add(LineHandler.class);
        return marker;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.protocols.api.ExtensibleHandler#wireExtensions(java.
     * lang.Class, java.util.List)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void wireExtensions(Class<?> interfaceName, List<?> extension) throws WiringException {

        if (interfaceName.equals(LineHandler.class)) {
            // add stats for all hooks
            for (int i = 0; i < extension.size(); i++) {
                LineHandler c = (LineHandler) extension.get(i);
                if (equals(c) == false) {
                    String cName = c.getClass().getName();
                    try {
                        lStats.put(cName, new LineHandlerStats(jmxName, cName));
                    } catch (Exception e) {
                        throw new WiringException("Unable to wire Hooks", e);
                    }
                }
            }
        }
    }

    @Override
    public void init(Configuration config) throws ConfigurationException {
        this.jmxName = config.getString("jmxName", getDefaultJMXName());        
    }

    @Override
    public void destroy() {
        Iterator<LineHandlerStats> it = lStats.values().iterator();
        while(it.hasNext()) {
            it.next().dispose();
        }
    }

    /**
     * Return default JMX Name if none is configured
     * 
     * @return defaultJMXName
     */
    protected abstract String getDefaultJMXName();
}
