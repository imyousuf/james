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
package org.apache.james.smtpserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.james.protocols.api.CommandHandler;
import org.apache.james.protocols.api.ExtensibleHandler;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.ResponseResultHandler;
import org.apache.james.protocols.api.WiringException;
import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPSession;

/**
 * Expose JMX statistics for {@link CommandHandler}
 *
 */
public class CommandHandlerResultJMXMonitor implements ResponseResultHandler<SMTPResponse, SMTPSession>, ExtensibleHandler {

    private Map<String, CommandHandlerStats> cStats = new HashMap<String, CommandHandlerStats>();

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ResponseResultHandler#onResponse(org.apache.james.protocols.api.ProtocolSession, org.apache.james.protocols.api.Response, org.apache.james.protocols.api.CommandHandler)
     */
    public Response onResponse(ProtocolSession session, SMTPResponse response, CommandHandler<SMTPSession> handler) {
        String name = handler.getClass().getName();
        CommandHandlerStats stats = cStats.get(name);
        if (stats != null) {
            stats.increment(response);
        }
        return response;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ExtensibleHandler#getMarkerInterfaces()
     */
    public List<Class<?>> getMarkerInterfaces() {
        List<Class<?>> marker = new ArrayList<Class<?>>();
        marker.add(CommandHandler.class);
        return marker;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void wireExtensions(Class<?> interfaceName, List<?> extension) throws WiringException {
        if (interfaceName.equals(CommandHandler.class)) {
            // add stats for all hooks
            for (int i = 0; i < extension.size(); i++ ) {
                CommandHandler c =  (CommandHandler) extension.get(i);
                if (equals(c) == false) {
                    String cName = c.getClass().getName();
                    try {
                        Collection<String> col = c.getImplCommands();
                        cStats.put(cName, new CommandHandlerStats(cName, col.toArray(new String[col.size()])));
                    } catch (Exception e) {
                        throw new WiringException("Unable to wire Hooks",  e);
                    }
                }
            }
        }
    }

}
