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

import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.main.ImapRequestHandler;
import org.apache.james.socket.mina.AbstractAsyncServer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;

/**
 * Async ImapServer which use MINA for socket handling
 *
 */
public class AsyncImapServer extends AbstractAsyncServer implements ImapConstants{

    private static final String softwaretype = "JAMES "+VERSION+" Server "; //+ Constants.SOFTWARE_VERSION;

    private String hello;
    private ImapProcessor processor;
    private ImapEncoder encoder;

    private ImapDecoder decoder;

    @Resource(name="imapDecoder")
    public void setImapDecoder(ImapDecoder decoder) {
        this.decoder = decoder;
    }
    
    @Resource(name="imapEncoder")
    public void setImapEncoder(ImapEncoder encoder) {
        this.encoder = encoder;
    }
    
    @Resource(name="imapProcessor")
    public void setImapProcessor(ImapProcessor processor) {
        this.processor = processor;
    }
    
    @Override
    public void doConfigure( final HierarchicalConfiguration configuration ) throws ConfigurationException {
        super.doConfigure(configuration);
        hello  = softwaretype + " Server " + getHelloName() + " is ready.";
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.mina.AbstractAsyncServer#getDefaultPort()
     */
    public int getDefaultPort() {
        return 143;
    }

 
    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.mina.AbstractAsyncServer#getServiceType()
     */
    public String getServiceType() {
        return "IMAP Service";
    }
    
    @Override
    protected IoHandler createIoHandler() {
        final ImapRequestHandler handler = new ImapRequestHandler(decoder, processor, encoder);
        return new ImapIoHandler(hello, handler, getLogger());
    }

    @Override
    protected DefaultIoFilterChainBuilder createIoFilterChainBuilder() {
        
        // just return an empty filterchain because we need no special protocol filter etc
        return new DefaultIoFilterChainBuilder();
    }

}
