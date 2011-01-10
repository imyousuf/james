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
package org.apache.james.pop3server.jmx;

import java.util.Collection;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.CommandHandler;
import org.apache.james.protocols.lib.jmx.AbstractCommandHandlerResultJMXMonitor;
import org.apache.james.protocols.lib.jmx.AbstractCommandHandlerStats;

/**
 * Gather JMX stats for {@link CommandHandler}
 *
 */
public class CommandHandlerResultJMXMonitor extends AbstractCommandHandlerResultJMXMonitor<POP3Response, POP3Session> implements Configurable{

    private String jmxName;


    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.AbstractCommandHandlerResultJMXMonitor#createCommandHandlerStats(org.apache.james.protocols.api.CommandHandler)
     */
    protected AbstractCommandHandlerStats<POP3Response> createCommandHandlerStats(CommandHandler<POP3Session> handler) throws Exception {
        Collection<String> col = handler.getImplCommands();
        String cName = handler.getClass().getName();

        return new POP3CommandHandlerStats(jmxName, cName, col.toArray(new String[col.size()]));
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        this.jmxName = config.getString("jmxName", "pop3server");
    }
}
