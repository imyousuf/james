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

package org.apache.james.pop3server.mina;

import org.apache.commons.logging.Log;
import org.apache.james.pop3server.POP3HandlerConfigurationData;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.socket.mina.AbstractIoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslContextFactory;

/**
 * This IoHandler handling the calling of ConnectHandler and LineHandlers
 * 
 *
 */
public class POP3IoHandler extends AbstractIoHandler{
    
    private Log logger;
    private POP3HandlerConfigurationData conf;
    private SslContextFactory contextFactory;

    public POP3IoHandler(ProtocolHandlerChain chain,
            POP3HandlerConfigurationData conf, Log logger) {
        this(chain,conf,logger,null);
    }
    
    public POP3IoHandler(ProtocolHandlerChain chain,
    		POP3HandlerConfigurationData conf, Log logger, SslContextFactory contextFactory) {
        super(chain);
        this.conf = conf;
        this.logger = logger;
        this.contextFactory = contextFactory;
    }

    /**
     * @see org.apache.mina.core.service.IoHandler#sessionIdle(org.apache.mina.core.session.IoSession,
     *      org.apache.mina.core.session.IdleStatus)
     */
    public void sessionIdle(IoSession session, IdleStatus status)
            throws Exception {
        logger.debug("Connection timed out");
        session.write("Connection timeout");
    }

    @Override
    protected ProtocolSession createSession(IoSession session) throws Exception {
        POP3Session pop3Session;
        if (contextFactory == null) {
            pop3Session = new POP3SessionImpl(conf, logger, session);
        } else {
            pop3Session = new POP3SessionImpl(conf, logger, session, contextFactory.newInstance());
        }
        return pop3Session;
    }

    @Override
    protected String getSessionKey() {
        return POP3SessionImpl.POP3SESSION;
    }

   
}
