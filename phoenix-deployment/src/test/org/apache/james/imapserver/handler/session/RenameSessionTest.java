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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.client.LoginCommand;
import org.apache.james.imapserver.client.LogoutClientCommand;
import org.apache.james.imapserver.client.RenameClientCommand;
import org.apache.james.imapserver.util.MessageGenerator;
import org.apache.james.mailboxmanager.MailboxManagerException;

public class RenameSessionTest extends AbstractSessionTest {

    String[] folders = { USER_MAILBOX_ROOT + ".INBOX",
            USER_MAILBOX_ROOT + ".test", USER_MAILBOX_ROOT + ".test1",
            USER_MAILBOX_ROOT + ".test1.test1a",
            USER_MAILBOX_ROOT + ".test1.test1b",
            USER_MAILBOX_ROOT + ".test2.test2a",
            USER_MAILBOX_ROOT + ".test2.test2b" };

    MimeMessage[] msgs = null;

    public void setUp() throws Exception {
        super.setUp();
        msgs = MessageGenerator.generateSimpleMessages(2);
        createFolders(folders);
        for (int i = 0; i < folders.length; i++) {
            appendMessagesClosed(folders[i], msgs);
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
        for (int i = 0; i < folders.length; i++) {
            assertFalse(folders[i] + " is still in use!", isOpen(folders[i]));
        }
    }

    public void testRenameSubfolder() throws ProtocolException, IOException,
            MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME, USER_PASSWORD));

        verifyCommand(new RenameClientCommand("test1.test1a", "test1.test1neu"));

        String[] expected = { USER_MAILBOX_ROOT + ".INBOX",
                USER_MAILBOX_ROOT + ".test", USER_MAILBOX_ROOT + ".test1",
                USER_MAILBOX_ROOT + ".test1.test1neu",
                USER_MAILBOX_ROOT + ".test1.test1b",
                USER_MAILBOX_ROOT + ".test2.test2a",
                USER_MAILBOX_ROOT + ".test2.test2b" };
        verifyFolderList(expected, getFolderNames());

        verifyCommand(new LogoutClientCommand());
    }

    public void testRenameParentfolder() throws ProtocolException, IOException,
            MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME, USER_PASSWORD));

        verifyCommand(new RenameClientCommand("test1", "test2"));

        String[] expected = { USER_MAILBOX_ROOT + ".INBOX",
                USER_MAILBOX_ROOT + ".test", USER_MAILBOX_ROOT + ".test2",
                USER_MAILBOX_ROOT + ".test2.test1a",
                USER_MAILBOX_ROOT + ".test2.test1b",
                USER_MAILBOX_ROOT + ".test2.test2a",
                USER_MAILBOX_ROOT + ".test2.test2b" };
        verifyFolderList(expected, getFolderNames());

        verifyCommand(new LogoutClientCommand());
    }

}
