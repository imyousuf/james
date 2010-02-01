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

package org.apache.james.socket.mina;

import java.util.LinkedList;
import java.util.List;

import org.apache.james.protocols.api.ConnectHandler;
import org.apache.james.protocols.api.LineHandler;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 * This abstract IoHandler handling the calling of ConnectHandler and LineHandlers
 * on the right events.
 * 
 *
 */
public abstract class AbstractIoHandler extends IoHandlerAdapter{
    
    private ProtocolHandlerChain chain;
    
    public AbstractIoHandler(ProtocolHandlerChain chain) {
        this.chain = chain;
    }

    /**
     * @see org.apache.mina.core.service.IoHandlerAdapter#messageReceived(org.apache.mina.core.session.IoSession, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public final void messageReceived(IoSession session, Object message)
            throws Exception {
        ProtocolSession pSession = (ProtocolSession) session.getAttribute(getSessionKey());
        LinkedList<LineHandler> lineHandlers = chain.getHandlers(LineHandler.class);
        
        IoBuffer buf = (IoBuffer) message;      
        byte[] line = new byte[buf.capacity()];
        buf.get(line, 0, line.length);
        
        if (lineHandlers.size() > 0) {
            
            // Maybe it would be better to use the ByteBuffer here
            ((LineHandler) lineHandlers.getLast()).onLine(pSession,line);
        }
    }

    /**
     * @see org.apache.mina.core.service.IoHandler#sessionCreated(org.apache.mina.core.session.IoSession)
     */
    public final void  sessionCreated(IoSession session) throws Exception { 
        // Add attribute
        session.setAttribute(getSessionKey(), createSession(session));
    }

    
    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        session.close(false);
    }

    /**
     * Create a new "protocol" session 
     * 
     * @param session ioSession
     * @return psession
     * @throws Exception
     */
    protected abstract ProtocolSession createSession(IoSession session) throws Exception;

    /**
     * Return the Key which is used to store the "protocol" session within the IoSession
     * 
     * @return key
     */
    protected abstract String getSessionKey();

    
    /**
     * @see org.apache.mina.core.service.IoHandler#sessionOpened(org.apache.mina.core.session.IoSession)
     */
    @SuppressWarnings("unchecked")
    public final void sessionOpened(IoSession session) throws Exception {
        List<ConnectHandler> connectHandlers = chain
                .getHandlers(ConnectHandler.class);
      
        if (connectHandlers != null) {
            for (int i = 0; i < connectHandlers.size(); i++) {
                connectHandlers.get(i).onConnect(
                        (ProtocolSession) session.getAttribute(getSessionKey()));
            }
        }    
    }
}
