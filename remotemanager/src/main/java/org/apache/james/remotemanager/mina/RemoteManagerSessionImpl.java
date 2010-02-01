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

package org.apache.james.remotemanager.mina;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.james.api.protocol.LineHandler;
import org.apache.james.api.protocol.Response;
import org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.james.remotemanager.mina.filter.RemoteManagerResponseFilter;
import org.apache.james.socket.mina.filter.FilterLineHandlerAdapter;
import org.apache.mina.core.session.IoSession;

public class RemoteManagerSessionImpl implements RemoteManagerSession {
    private Log logger;
    private IoSession session;
    private Map<String, Object> state = new HashMap<String, Object>();
    private RemoteManagerHandlerConfigurationData config;
    private int lineHandlerCount = 0;
    private InetSocketAddress socketAddress;

    public final static String REMOTEMANAGER_SESSION = "REMOTEMANAGER_SESSION";

    
    public RemoteManagerSessionImpl(RemoteManagerHandlerConfigurationData config, Log logger, IoSession session) {
        this.logger = logger;
        this.session = session;
        this.config = config;
        this.socketAddress = (InetSocketAddress) session.getRemoteAddress();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.remotemanager.RemoteManagerSession#getState()
     */
    public Map<String, Object> getState() {
        return state;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.protocol.LogEnabledSession#getLogger()
     */
    public Log getLogger() {
        return logger;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.protocol.LogEnabledSession#resetState()
     */
    public void resetState() {
        state.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.apache.james.remotemanager.RemoteManagerSession#
     * getAdministrativeAccountData()
     */
    public Map<String, String> getAdministrativeAccountData() {
        return config.getAdministrativeAccountData();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerSession#popLineHandler()
     */
    public void popLineHandler() {
        session.getFilterChain().remove("lineHandler" + lineHandlerCount);
        lineHandlerCount--;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerSession#pushLineHandler(org.apache.james.remotemanager.LineHandler)
     */
    public void pushLineHandler(LineHandler<RemoteManagerSession> overrideCommandHandler) {
        lineHandlerCount++;
        session.getFilterChain().addAfter(RemoteManagerResponseFilter.NAME, "lineHandler" + lineHandlerCount, new FilterLineHandlerAdapter<RemoteManagerSession>(overrideCommandHandler, REMOTEMANAGER_SESSION));
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#getRemoteHost()
     */
    public String getRemoteHost() {
        return socketAddress.getHostName();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return socketAddress.getAddress().getHostAddress();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#writeResponse(org.apache.james.api.protocol.Response)
     */
    public void writeResponse(Response response) {
        if (session.isConnected()) {
            session.write(response);    
        }
    }
}
