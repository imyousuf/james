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

package org.apache.james.imapserver.mina;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.main.ImapRequestHandler;
import org.apache.james.imap.main.ImapSessionImpl;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.handler.stream.StreamIoHandler;

/**
 * IoHandler which process Imap commands
 *
 */
public class ImapIoHandler extends StreamIoHandler{

    private final Log logger;

    private final String hello;

    private final ImapRequestHandler handler;

    private final static String IMAP_SESSION = "IMAP_SESSION"; 
    
    public ImapIoHandler(String hello, ImapRequestHandler handler, Log logger) {
        this.logger = logger;
        this.hello = hello;
        this.handler = handler;
        
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
    	logger.error("Error while processing imap request",cause);
    	
    	// logout on error not sure if that is the best way to handle it
        final ImapSessionImpl imapSession = (ImapSessionImpl) session.getAttribute(IMAP_SESSION);     
        if (imapSession != null) imapSession.logout();
        session.close(false);
        
    	super.exceptionCaught(session, cause);
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        
        // create the imap session and store it in the IoSession for later usage
        final ImapSessionImpl imapsession = new ImapSessionImpl();
        imapsession.setLog(logger);
        
        session.setAttribute(IMAP_SESSION, imapsession);
        
        super.sessionCreated(session);
    }
    

    @Override
    public void sessionOpened(IoSession session) {
        // write hello to client
        session.write(IoBuffer.wrap((ImapConstants.UNTAGGED + " OK " + hello +" " + new String(ImapConstants.BYTES_LINE_END)).getBytes()));
        
        super.sessionOpened(session);
    }

    @Override
    protected void processStreamIo(final IoSession session, final InputStream in, final OutputStream out) {  
        
        // it would prolly make sense to use a thread pool...
        new Thread(new Runnable() {
        
            public void run() {
                final ImapSessionImpl imapSession = (ImapSessionImpl) session.getAttribute(IMAP_SESSION);

                // handle requests in a loop
                while (handler.handleRequest(in, out, imapSession));
                if (imapSession != null) imapSession.logout();
                session.close(false);
            }

        }).start();

    }

}
