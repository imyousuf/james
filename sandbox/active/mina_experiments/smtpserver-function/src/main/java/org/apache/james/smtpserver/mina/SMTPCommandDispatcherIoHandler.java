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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.ConnectHandler;
import org.apache.james.smtpserver.SMTPConfiguration;
import org.apache.james.smtpserver.SMTPHandlerChain;
import org.apache.james.smtpserver.SMTPRequest;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPRetCode;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.core.UnknownCmdHandler;
import org.apache.james.socket.shared.AbstractCommandDispatcher;
import org.apache.james.socket.shared.ExtensibleHandler;
import org.apache.james.socket.shared.WiringException;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslContextFactory;

public class SMTPCommandDispatcherIoHandler extends
        AbstractCommandDispatcher<CommandHandler> implements ExtensibleHandler,
        IoHandler {
    private final static String SMTP_SESSION = "com.googlecode.asyncmail.smtpserver.SMTPCommandDispatcherIoHandler.SMTP_SESSION";
    private final UnknownCmdHandler unknownCmdHandler = new UnknownCmdHandler();
    private final static String[] mandatoryCommands = { "MAIL", "RCPT", "QUIT" };
    private Log logger;
    private SMTPHandlerChain chain;
    private SMTPConfiguration conf;
    private SslContextFactory contextFactory;

    public SMTPCommandDispatcherIoHandler(SMTPHandlerChain chain,
            SMTPConfiguration conf, Log logger) {
        this(chain,conf,logger,null);
    }
    
    public SMTPCommandDispatcherIoHandler(SMTPHandlerChain chain,
            SMTPConfiguration conf, Log logger, SslContextFactory contextFactory) {
        this.chain = chain;
        this.conf = conf;
        this.logger = logger;
        this.contextFactory = contextFactory;
    }
    
    
    public void init() throws Exception {
        wireCommandHandler();
    }

    /**
     * @see org.apache.james.socket.shared.AbstractCommandDispatcher#getLog()
     */
    protected Log getLog() {
        return logger;
    }

    @Override
    protected List<String> getMandatoryCommands() {
        return Arrays.asList(mandatoryCommands);
    }

    /**
     * @see org.apache.james.socket.shared.AbstractCommandDispatcher#getUnknownCommandHandler()
     */
    protected CommandHandler getUnknownCommandHandler() {
        return unknownCmdHandler;
    }

    /**
     * @see org.apache.james.socket.shared.AbstractCommandDispatcher#getUnknownCommandHandlerIdentifier()
     */
    protected String getUnknownCommandHandlerIdentifier() {
        return UnknownCmdHandler.UNKNOWN_COMMAND;
    }

    /**
     * @see org.apache.james.socket.shared.ExtensibleHandler#getMarkerInterfaces()
     */
    public List<Class<?>> getMarkerInterfaces() {
        List<Class<?>> res = new LinkedList<Class<?>>();
        res.add(CommandHandler.class);
        res.add(ConnectHandler.class);
        return res;
    }


    protected void wireCommandHandler() throws WiringException {
        List<CommandHandler> chandlers = chain.getHandlers(CommandHandler.class);
        List<Class<?>> markerInterfaces = getMarkerInterfaces();
        for (int i = 0;  i < markerInterfaces.size(); i++) {
            wireExtensions(markerInterfaces.get(i), chandlers);
        }
    }
    /**
     * @see org.apache.mina.core.service.IoHandler#exceptionCaught(org.apache.mina.core.session.IoSession,
     *      java.lang.Throwable)
     */
    public void exceptionCaught(IoSession session, Throwable exception)
            throws Exception {
    	exception.printStackTrace();
        logger.error("Caught exception: " + session.getCurrentWriteMessage(),
                exception);
        // just close session
        session.close(true);
    }

    /**
     * @see org.apache.mina.core.service.IoHandler#messageReceived(org.apache.mina.core.session.IoSession,
     *      java.lang.Object)
     */
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        if (message instanceof SMTPRequest) {
            SMTPRequest request = (SMTPRequest) message;
            SMTPSession smtpSession = (SMTPSession) session
                    .getAttribute(SMTP_SESSION);
            List<CommandHandler> commandHandlers = getCommandHandlers(request
                    .getCommand(), smtpSession);
            // fetch the command handlers registered to the command
            if (commandHandlers == null) {
                // end the session
                SMTPResponse resp = new SMTPResponse(SMTPRetCode.LOCAL_ERROR,
                        "Local configuration error: unable to find a command handler.");
                resp.setEndSession(true);
                session.write(resp);
            } else {
                int count = commandHandlers.size();
                for (int i = 0; i < count; i++) {
                    SMTPResponse response = commandHandlers.get(i).onCommand(
                            smtpSession, request);

                    // if the response is received, stop processing of command
                    // handlers
                    if (response != null) {
                        session.write(response);
                        break;
                    }

                    // NOTE we should never hit this line, otherwise we ended
                    // the
                    // CommandHandlers with
                    // no responses.
                    // (The note is valid for i == count-1)
                }

            }

        } else {
            logger.error("Invalid message object");
        }

    }

    /**
     * Not implemented
     */
    public void messageSent(IoSession session, Object message) throws Exception {
        // Nothing todo here
    }

    /**
     * Not implemented
     */
    public void sessionClosed(IoSession session) throws Exception {
        // Nothing todo here

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
