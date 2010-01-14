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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.james.api.protocol.ProtocolHandlerChain;
import org.apache.james.pop3server.ConnectHandler;
import org.apache.james.pop3server.LineHandler;
import org.apache.james.pop3server.POP3HandlerConfigurationData;
import org.apache.james.pop3server.POP3Request;
import org.apache.james.pop3server.POP3Session;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslContextFactory;

/**
 * This IoHandler handling the calling of ConnectHandler and LineHandlers
 * 
 *
 */
public class POP3IoHandler extends IoHandlerAdapter{
    private final static String POP3_SESSION = "org.apache.james.pop3server.mina.POP3IoHandler.POP3_SESSION";
    
    private Log logger;
    private ProtocolHandlerChain chain;
    private POP3HandlerConfigurationData conf;
    private SslContextFactory contextFactory;

    public POP3IoHandler(ProtocolHandlerChain chain,
            POP3HandlerConfigurationData conf, Log logger) {
        this(chain,conf,logger,null);
    }
    
    public POP3IoHandler(ProtocolHandlerChain chain,
    		POP3HandlerConfigurationData conf, Log logger, SslContextFactory contextFactory) {
        this.chain = chain;
        this.conf = conf;
        this.logger = logger;
        this.contextFactory = contextFactory;
    }

    /**
     * @see org.apache.mina.core.service.IoHandlerAdapter#messageReceived(org.apache.mina.core.session.IoSession, java.lang.Object)
     */
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        POP3Session pop3Session = (POP3Session) session.getAttribute(POP3_SESSION);
        LinkedList<LineHandler> lineHandlers = chain.getHandlers(LineHandler.class);
        if (lineHandlers.size() > 0) {
            // thats not really optimal but it allow us to keep things as generic as possible
            // Will prolly get refactored later
            String line = ((POP3Request) message).toString();
            ((LineHandler) lineHandlers.getLast()).onLine(pop3Session, line);
        }
    }

    /**
     * @see org.apache.mina.core.service.IoHandler#exceptionCaught(org.apache.mina.core.session.IoSession,
     *      java.lang.Throwable)
     */
    public void exceptionCaught(IoSession session, Throwable exception)
            throws Exception {
        logger.error("Caught exception: " + session.getCurrentWriteMessage(),
                exception);
        // just close session
        session.close(true);
    }

    /**
     * @see org.apache.mina.core.service.IoHandler#sessionCreated(org.apache.mina.core.session.IoSession)
     */
    public void sessionCreated(IoSession session) throws Exception {
        POP3Session pop3Session;
        if (contextFactory == null) {
        	pop3Session = new POP3SessionImpl(conf, logger, session);
        } else {
        	pop3Session = new POP3SessionImpl(conf, logger, session, contextFactory.newInstance());
        }
        
        // Add attribute
        session.setAttribute(POP3_SESSION,pop3Session);
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

    /**
     * @see org.apache.mina.core.service.IoHandler#sessionOpened(org.apache.mina.core.session.IoSession)
     */
    public void sessionOpened(IoSession session) throws Exception {
        List<ConnectHandler> connectHandlers = chain
                .getHandlers(ConnectHandler.class);
      
        if (connectHandlers != null) {
            for (int i = 0; i < connectHandlers.size(); i++) {
                connectHandlers.get(i).onConnect(
                        (POP3Session) session.getAttribute(POP3_SESSION));
            }
        }    
    }
}
