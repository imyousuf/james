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

import org.apache.james.socket.DelegatingJamesHandler;

/**
 * <p>Accepts SMTP connections on a server socket and dispatches them to SMTPHandlers.</p>
 *
 * <p>Also responsible for loading and parsing SMTP specific configuration.</p>
 *
 * @version 1.1.0, 06/02/2001
 */
/*
 * IMPORTANT: DelegatingSMTPServer extends AbstractJamesService.  If you implement ANY
 * lifecycle methods, you MUST call super.<method> as well.
 */
public class DelegatingSMTPServer extends SMTPServer {

    /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
     */
    public Object newInstance() throws Exception {
        SMTPProtocolHandler protocolHandler = new SMTPProtocolHandler();
        //pass the handler chain to every SMTPhandler
        protocolHandler.setHandlerChain(handlerChain);
        return new DelegatingJamesHandler(protocolHandler);
    }
    
    /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
     */
    public Class getCreatedClass() {
        return DelegatingJamesHandler.class;
    }
}
