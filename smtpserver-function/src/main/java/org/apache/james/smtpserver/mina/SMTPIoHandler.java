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

package org.apache.james.smtpserver.mina;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.james.api.protocol.ProtocolHandlerChain;
import org.apache.james.smtpserver.protocol.ConnectHandler;
import org.apache.james.smtpserver.protocol.LineHandler;
import org.apache.james.smtpserver.protocol.SMTPConfiguration;
import org.apache.james.smtpserver.protocol.SMTPRequest;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslContextFactory;

/**
 * This IoHandler handling the calling of ConnectHandler and LineHandlers
 * 
 *
 */
public class SMTPIoHandler extends IoHandlerAdapter{
    private final static String SMTP_SESSION = "org.apache.james.smtpserver.mina.SMTPIoHandler.SMTP_SESSION";
    
    private Log logger;
    private ProtocolHandlerChain chain;
    private SMTPConfiguration conf;
    private SslContextFactory contextFactory;

    public SMTPIoHandler(ProtocolHandlerChain chain,
            SMTPConfiguration conf, Log logger) {
        this(chain,conf,logger,null);
    }
    
    public SMTPIoHandler(ProtocolHandlerChain chain,
            SMTPConfiguration conf, Log logger, SslContextFactory contextFactory) {
        this.chain = chain;
        this.conf = conf;
        this.logger = logger;
        this.contextFactory = contextFactory;
    }
    

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getLog()
     */
    protected Log getLog() {
        return logger;
    }



    /**
     * @see org.apache.mina.core.service.IoHandlerAdapter#messageReceived(org.apache.mina.core.session.IoSession, java.lang.Object)
     */
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        SMTPSession smtpSession = (SMTPSession) session.getAttribute(SMTP_SESSION);
        LinkedList<LineHandler> lineHandlers = chain.getHandlers(LineHandler.class);
        if (lineHandlers.size() > 0) {
            // thats not really optimal but it allow us to keep things as generic as possible
            // Will prolly get refactored later
            byte[] line = ((SMTPRequest) message).toString().getBytes("US-ASCII");
            ((LineHandler) lineHandlers.getLast()).onLine(smtpSession, line);
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
        SMTPSession smtpSession;
        if (contextFactory == null) {
            smtpSession= new SMTPSessionImpl(conf, logger, session);
        } else {
            smtpSession= new SMTPSessionImpl(conf, logger, session, contextFactory.newInstance());
        }
        // Add attributes

        session.setAttribute(SMTP_SESSION,smtpSession);
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
                        (SMTPSession) session.getAttribute(SMTP_SESSION));
            }
        }    
    }
}
