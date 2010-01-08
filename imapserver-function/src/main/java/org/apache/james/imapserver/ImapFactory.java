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

package org.apache.james.imapserver;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.main.ImapRequestHandler;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;

public class ImapFactory {

    private final ImapEncoder encoder;
    private final ImapDecoder decoder;
    private ImapProcessor processor;
    private MailboxManager mailboxManager;

    public ImapFactory () {
        decoder = new DefaultImapDecoderFactory().buildImapDecoder();
        encoder = new DefaultImapEncoderFactory().buildImapEncoder();
    } 
    
    @Resource(name="mailboxmanager")
    public void setMailboxManager(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }
    
    public ImapRequestHandler createHandler() { 
        return new ImapRequestHandler(decoder, processor, encoder);
    }

    @PostConstruct
    public void init() {
        processor = DefaultImapProcessorFactory.createDefaultProcessor(mailboxManager);
    }
    
    /**
     * This is required until James supports IoC assembly.
     * @return the mailbox
     */
    public final MailboxManager getMailbox() {
        return mailboxManager;
    }
    
}
