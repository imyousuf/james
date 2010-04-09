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
package org.apache.james.pop3server.netty;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.pop3server.POP3HandlerConfigurationData;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.socket.netty.AbstractNettySession;
import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * {@link POP3Session} implementation which use Netty
 *
 */
public class POP3NettySession extends AbstractNettySession implements POP3Session {
    private POP3HandlerConfigurationData configData;

    private Map<String, Object> state = new HashMap<String, Object>();

    private int handlerState;

    private Mailbox mailbox;

    public POP3NettySession(POP3HandlerConfigurationData configData, Log logger, ChannelHandlerContext handlerContext) {
        super(logger, handlerContext);
        this.configData = configData;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.pop3server.POP3Session#getConfigurationData()
     */
    public POP3HandlerConfigurationData getConfigurationData() {
        return configData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.pop3server.POP3Session#getHandlerState()
     */
    public int getHandlerState() {
        return handlerState;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.protocol.TLSSupportedSession#getState()
     */
    public Map<String, Object> getState() {
        return state;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.pop3server.POP3Session#setHandlerState(int)
     */
    public void setHandlerState(int handlerState) {
        this.handlerState = handlerState;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.api.protocol.TLSSupportedSession#resetState()
     */
    public void resetState() {
        state.clear();

        setHandlerState(AUTHENTICATION_READY);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.pop3server.POP3Session#getUserMailbox()
     */
    public Mailbox getUserMailbox() {
        return mailbox;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.pop3server.POP3Session#setUserMailbox(org.apache.james.imap.mailbox.Mailbox)
     */
    public void setUserMailbox(Mailbox mailbox) {
        this.mailbox = mailbox;
    }

}
