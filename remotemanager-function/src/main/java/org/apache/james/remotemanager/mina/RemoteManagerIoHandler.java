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

import org.apache.commons.logging.Log;
import org.apache.james.api.protocol.ProtocolSession;
import org.apache.james.api.protocol.ProtocolHandlerChain;
import org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.james.socket.mina.AbstractIoHandler;
import org.apache.mina.core.session.IoSession;

public class RemoteManagerIoHandler extends AbstractIoHandler{
    
    private Log logger;
    private RemoteManagerHandlerConfigurationData config;

    public RemoteManagerIoHandler(RemoteManagerHandlerConfigurationData config, ProtocolHandlerChain chain, Log logger) {
        super(chain);
        this.logger = logger;
        this.config = config;
    }

    /**
     * @see org.apache.mina.core.service.IoHandler#exceptionCaught(org.apache.mina.core.session.IoSession,
     *      java.lang.Throwable)
     */
    public void exceptionCaught(IoSession session, Throwable exception)
            throws Exception {
        logger.error("Caught exception: " + session.getCurrentWriteMessage(),
                exception);
    }

    @Override
    protected ProtocolSession createSession(IoSession session) throws Exception {
        RemoteManagerSession rSession  = new RemoteManagerSessionImpl(config, logger, session);
        rSession.getState().put(RemoteManagerSession.CURRENT_USERREPOSITORY, "LocalUsers");
        return rSession;
    }

    @Override
    protected String getSessionKey() {
        return RemoteManagerSessionImpl.REMOTEMANAGER_SESSION;
    }

}
