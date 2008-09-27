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


import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.main.ImapRequestHandler;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.mailboxmanager.torque.DefaultMailboxManagerProvider;
import org.apache.james.mailboxmanager.torque.DefaultMailboxManager;
import org.apache.james.mailboxmanager.torque.DefaultUserManager;
import org.apache.james.services.FileSystem;
import org.apache.james.user.impl.file.FileUserMetaDataRepository;

public class DefaultImapFactory {

    private final ImapEncoder encoder;
    private final ImapDecoder decoder;
    private final ImapProcessor processor;
    
    public DefaultImapFactory(FileSystem fileSystem, UsersRepository users, Logger logger) {
        super();
        decoder = new DefaultImapDecoderFactory().buildImapDecoder();
        encoder = new DefaultImapEncoderFactory().buildImapEncoder();
        processor = DefaultImapProcessorFactory.createDefaultProcessor(
                new DefaultMailboxManagerProvider(
                        new DefaultMailboxManager(new DefaultUserManager(
                                new FileUserMetaDataRepository("var/users"), users), fileSystem, logger)));
    }



    public ImapRequestHandler createHandler()
    { 
        return new ImapRequestHandler(decoder, processor, encoder);
    }
}
