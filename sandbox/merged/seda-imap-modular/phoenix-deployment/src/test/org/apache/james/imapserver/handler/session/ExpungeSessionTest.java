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


package org.apache.james.imapserver.handler.session;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.client.ExpungeClientCommand;
import org.apache.james.imapserver.client.LoginCommand;
import org.apache.james.imapserver.client.SelectCommand;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.util.MessageGenerator;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.TestUtil;

public class ExpungeSessionTest extends AbstractSessionTest {

    String[] onlyInbox = { USER_INBOX };

    MimeMessage[] msgs = null;

    long[] uids = null;

    public void setUp() throws MailboxException, MessagingException,
            IOException, MailboxManagerException {
        super.setUp();
        createFolders(onlyInbox);
    }

    public void testExpungeOneMessage() throws MessagingException,
            MailboxManagerException, ProtocolException, IOException {
        msgs = MessageGenerator.generateSimpleMessages(1);
        msgs[0].setFlag(Flags.Flag.DELETED, true);
        addUIDMessagesOpen(USER_INBOX, msgs);
        verifyCommand(new LoginCommand(USER_NAME, USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs,
                getUidValidity(USER_INBOX)));
        verifyCommandOrdered(new ExpungeClientCommand(msgs));
        assertEquals(0,getMessages(USER_INBOX).length);
    }

    public void testExpunge3Messages() throws MessagingException,
            MailboxManagerException, ProtocolException, IOException {
        msgs = MessageGenerator.generateSimpleMessages(3);
        msgs[0].setFlag(Flags.Flag.DELETED, true);
        msgs[1].setFlag(Flags.Flag.DELETED, true);
        msgs[2].setFlag(Flags.Flag.DELETED, true);
        addUIDMessagesOpen(USER_INBOX, msgs);
        verifyCommand(new LoginCommand(USER_NAME, USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs,
                getUidValidity(USER_INBOX)));
        verifyCommandOrdered(new ExpungeClientCommand(msgs));
        assertEquals(0,getMessages(USER_INBOX).length);
    }

    public void testExpunge4Of6Messages() throws MessagingException,
            MailboxManagerException, ProtocolException, IOException {
        msgs = MessageGenerator.generateSimpleMessages(6);
        msgs[0].setFlag(Flags.Flag.DELETED, true);
        msgs[2].setFlag(Flags.Flag.DELETED, true);
        msgs[3].setFlag(Flags.Flag.DELETED, true);
        msgs[5].setFlag(Flags.Flag.DELETED, true);
        addUIDMessagesOpen(USER_INBOX, msgs);
        verifyCommand(new LoginCommand(USER_NAME, USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs,
                getUidValidity(USER_INBOX)));
        verifyCommandOrdered(new ExpungeClientCommand(msgs));
        MimeMessage[] currentMsgs=getMessages(USER_INBOX);
        assertEquals(2,currentMsgs.length);
        Collection existing=Arrays.asList(currentMsgs);
        Collection expected=Arrays.asList(new MimeMessage[] {msgs[1],msgs[4]});
        assertTrue(TestUtil.messageSetsEqual(existing,expected));
    }
}
