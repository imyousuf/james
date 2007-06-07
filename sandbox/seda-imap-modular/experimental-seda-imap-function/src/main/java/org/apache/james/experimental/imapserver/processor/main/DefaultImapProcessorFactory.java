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

package org.apache.james.experimental.imapserver.processor.main;

import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.experimental.imapserver.processor.base.ImapResponseMessageProcessor;
import org.apache.james.experimental.imapserver.processor.base.UnknownRequestImapProcessor;
import org.apache.james.experimental.imapserver.processor.imap4rev1.Imap4Rev1ProcessorFactory;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.UsersRepository;


/**
 * TODO: perhaps this should be a POJO
 */
public class DefaultImapProcessorFactory {

    public static final ImapProcessor createDefaultProcessor(final UsersRepository usersRepository,
            final MailboxManagerProvider mailboxManagerProvider) {
        final UnknownRequestImapProcessor unknownRequestImapProcessor = new UnknownRequestImapProcessor();
        final ImapProcessor imap4rev1Chain = Imap4Rev1ProcessorFactory.createDefaultChain(unknownRequestImapProcessor, usersRepository, mailboxManagerProvider);
        final ImapProcessor result = new ImapResponseMessageProcessor(imap4rev1Chain);
        return result;
    }
    
}
