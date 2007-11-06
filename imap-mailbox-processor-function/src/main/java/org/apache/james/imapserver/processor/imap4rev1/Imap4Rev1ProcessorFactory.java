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

package org.apache.james.imapserver.processor.imap4rev1;

import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.UsersRepository;

/**
 * TODO: perhaps this should be a POJO
 */
public class Imap4Rev1ProcessorFactory {

    public static final ImapProcessor createDefaultChain(
            final ImapProcessor chainEndProcessor, final UsersRepository users,
            final MailboxManagerProvider mailboxManagerProvider, 
            final StatusResponseFactory statusResponseFactory) {
        
        final LogoutProcessor logoutProcessor = new LogoutProcessor(
                chainEndProcessor, statusResponseFactory);
        final CapabilityProcessor capabilityProcessor = new CapabilityProcessor(
                logoutProcessor, statusResponseFactory);
        final CheckProcessor checkProcessor = new CheckProcessor(
                capabilityProcessor, statusResponseFactory);
        final LoginProcessor loginProcessor = new LoginProcessor(
                checkProcessor, users, statusResponseFactory);
        final RenameProcessor renameProcessor = new RenameProcessor(
                loginProcessor, mailboxManagerProvider, statusResponseFactory);
        final DeleteProcessor deleteProcessor = new DeleteProcessor(
                renameProcessor, mailboxManagerProvider, statusResponseFactory);
        final CreateProcessor createProcessor = new CreateProcessor(
                deleteProcessor, mailboxManagerProvider,statusResponseFactory);
        final CloseProcessor closeProcessor = new CloseProcessor(
                createProcessor,statusResponseFactory);
        final UnsubscribeProcessor unsubscribeProcessor = new UnsubscribeProcessor(
                closeProcessor, mailboxManagerProvider, statusResponseFactory);
        final SubscribeProcessor subscribeProcessor = new SubscribeProcessor(
                unsubscribeProcessor, mailboxManagerProvider, statusResponseFactory);
        final CopyProcessor copyProcessor = new CopyProcessor(
                subscribeProcessor, mailboxManagerProvider, statusResponseFactory);
        final AuthenticateProcessor authenticateProcessor = new AuthenticateProcessor(
                copyProcessor, statusResponseFactory);
        final ExpungeProcessor expungeProcessor = new ExpungeProcessor(
                authenticateProcessor, mailboxManagerProvider, statusResponseFactory);
        final ExamineProcessor examineProcessor = new ExamineProcessor(
                expungeProcessor, mailboxManagerProvider, statusResponseFactory);
        final AppendProcessor appendProcessor = new AppendProcessor(
                examineProcessor, mailboxManagerProvider, statusResponseFactory);
        final StoreProcessor storeProcessor = new StoreProcessor(
                appendProcessor, statusResponseFactory);
        final NoopProcessor noopProcessor = new NoopProcessor(storeProcessor, statusResponseFactory);
        final StatusProcessor statusProcessor = new StatusProcessor(
                noopProcessor, mailboxManagerProvider, statusResponseFactory);
        final LSubProcessor lsubProcessor = new LSubProcessor(statusProcessor,
                mailboxManagerProvider, statusResponseFactory);
        final ListProcessor listProcessor = new ListProcessor(lsubProcessor,
                mailboxManagerProvider, statusResponseFactory);
        final SearchProcessor searchProcessor = new SearchProcessor(
                listProcessor, statusResponseFactory);
        final SelectProcessor selectProcessor = new SelectProcessor(
                searchProcessor, mailboxManagerProvider, statusResponseFactory);
        final ImapProcessor result = new FetchProcessor(selectProcessor, statusResponseFactory);
        return result;
    }
}
