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

package org.apache.james.imapserver.processor.main;

import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapProcessorFactory;
import org.apache.james.imapserver.processor.base.ImapResponseMessageProcessor;
import org.apache.james.imapserver.processor.base.UnknownRequestImapProcessor;
import org.apache.james.imapserver.processor.imap4rev1.Imap4Rev1ProcessorFactory;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.UsersRepository;

/**
 * 
 */
public class DefaultImapProcessorFactory implements ImapProcessorFactory {

    public static final ImapProcessor createDefaultProcessor(final UsersRepository usersRepository,
            final MailboxManagerProvider mailboxManagerProvider) {
        final UnknownRequestImapProcessor unknownRequestImapProcessor = new UnknownRequestImapProcessor();
        final ImapProcessor imap4rev1Chain = Imap4Rev1ProcessorFactory.createDefaultChain(unknownRequestImapProcessor, usersRepository, mailboxManagerProvider);
        final ImapProcessor result = new ImapResponseMessageProcessor(imap4rev1Chain);
        return result;
    }
    
    private UsersRepository usersRepository;
    private MailboxManagerProvider mailboxManagerProvider;
    
    public final void configure(UsersRepository usersRepository, MailboxManagerProvider mailboxManagerProvider) {
        setUsersRepository(usersRepository);
        setMailboxManagerProvider(mailboxManagerProvider);
    }
    
    public final MailboxManagerProvider getMailboxManagerProvider() {
        return mailboxManagerProvider;
    }
    
    public final void setMailboxManagerProvider(
            MailboxManagerProvider mailboxManagerProvider) {
        this.mailboxManagerProvider = mailboxManagerProvider;
    }
    
    public final UsersRepository getUsersRepository() {
        return usersRepository;
    }
    
    public final void setUsersRepository(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public ImapProcessor buildImapProcessor() {
        return createDefaultProcessor(usersRepository, mailboxManagerProvider);
    }
}
