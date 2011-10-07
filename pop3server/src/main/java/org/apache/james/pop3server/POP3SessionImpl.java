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
package org.apache.james.pop3server;

import org.apache.james.mailbox.MessageManager;
import org.apache.james.protocols.api.AbstractSession;
import org.apache.james.protocols.api.ProtocolTransport;
import org.apache.james.protocols.api.Response;
import org.slf4j.Logger;

/**
 * {@link POP3Session} implementation which use Netty
 */
public class POP3SessionImpl extends AbstractSession implements POP3Session {
    private POP3HandlerConfiguration configData;

    private int handlerState;

    private MessageManager mailbox;

    
    public POP3SessionImpl(Logger logger, ProtocolTransport transport, POP3HandlerConfiguration configData) {
        super(logger, transport);
        this.configData = configData;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getConfigurationData()
     */
    public POP3HandlerConfiguration getConfigurationData() {
        return configData;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getHandlerState()
     */
    public int getHandlerState() {
        return handlerState;
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#setHandlerState(int)
     */
    public void setHandlerState(int handlerState) {
        this.handlerState = handlerState;
    }

    /**
     * @see org.apache.james.protocols.api.ProtocolSession#resetState()
     */
    public void resetState() {
        getState().clear();

        setHandlerState(AUTHENTICATION_READY);
    }

    /**
     * @see org.apache.james.pop3server.POP3Session#getUserMailbox()
     */
    public MessageManager getUserMailbox() {
        return mailbox;
    }

    /**
     * @see
     * org.apache.james.pop3server.POP3Session#setUserMailbox(MessageManager)
     */
    public void setUserMailbox(MessageManager mailbox) {
        this.mailbox = mailbox;
    }

    @Override
    public Response newLineTooLongResponse() {
        return null;
    }

    @Override
    public Response newFatalErrorResponse() {
        return null;
    }




}
