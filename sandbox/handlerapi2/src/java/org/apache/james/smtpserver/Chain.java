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




package org.apache.james.smtpserver;

import java.util.Iterator;

/**
 * The Chain which contain the handlers for the current command or state
 * 
 */
public class Chain {

    private Iterator handlers;

    /**
     * The Chain which contain the handlers for the current command or state
     * 
     * @param handlers The iterator which contains all handler for the current command or state
     */
    public Chain(Iterator handlers) {
    this.handlers = handlers;
    }

    /**
     * Call the next handler in the chain
     * 
     * @param session The SMTPSession
     */
    public void doChain(SMTPSession session) {
    
    // should never happen
    if (handlers == null)
        return;
    
    if (handlers.hasNext()) {
        Object handler = handlers.next();

        if (handler instanceof ConnectHandler) {
        ((ConnectHandler) handler).onConnect(session, this);
        } else if (handler instanceof CommandHandler) {
        // reset the idle timeout
        session.getWatchdog().reset();

        ((CommandHandler) handler).onCommand(session, this);
        } else if (handler instanceof MessageHandler) {
        ((MessageHandler) handler).onMessage(session, this);
        }
    }
    }
}
