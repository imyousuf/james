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
package org.apache.james.pop3server.mailbox;

import java.io.IOException;

import javax.annotation.Resource;

import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.protocols.pop3.POP3Session;
import org.apache.james.protocols.pop3.mailbox.Mailbox;
import org.apache.james.protocols.pop3.mailbox.MailboxFactory;

public class JamesMailboxFactory implements MailboxFactory {

    private MailboxManager manager;

    @Resource(name = "mailboxmanager")
    public void setMailboxManager(MailboxManager manager) {
        this.manager = manager;
    }

    @Override
    public Mailbox getMailbox(POP3Session session, String password) throws IOException {
        MailboxSession mSession = null;
        try {
            mSession = manager.login(session.getUser(), password, session.getLogger());
            manager.startProcessingRequest(mSession);
            MessageManager mailbox = manager.getMailbox(MailboxPath.inbox(mSession), mSession);
            return new MailboxAdapter(manager, mailbox, mSession);
        } catch (BadCredentialsException e) {
            return null;
        } catch (MailboxException e) {
            throw new IOException("Unable to access mailbox for user " + session.getUser(), e);
        } finally {
            if (mSession != null) {
                manager.endProcessingRequest(mSession);
            }
        }

    }

}
