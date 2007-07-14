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
import org.apache.james.imapserver.client.CreateClientCommand;
import org.apache.james.imapserver.client.LoginCommand;
import org.apache.james.imapserver.client.LogoutClientCommand;
import org.apache.james.imapserver.client.SelectCommand;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;

public class CreateSessionTest extends AbstractSessionTest {
    
    String[] folders = {USER_MAILBOX_ROOT+".INBOX",USER_MAILBOX_ROOT+".test",USER_MAILBOX_ROOT+".test1",USER_MAILBOX_ROOT+".test1.test1a",USER_MAILBOX_ROOT+".test1.test1b",USER_MAILBOX_ROOT+".test2.test2a",USER_MAILBOX_ROOT+".test2.test2b"};
    
    public void setUp() throws MailboxException, MessagingException, IOException, MailboxManagerException {
        super.setUp();
        createFolders(folders);
    }
    
    public void testCreateSelect() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        
        verifyCommand(new CreateClientCommand("Drafts"));
        assertTrue(folderExists(USER_MAILBOX_ROOT+".Drafts"));
        
        verifyCommand(new SelectCommand("Drafts", new MimeMessage[0],getUidValidity(USER_MAILBOX_ROOT+".Drafts")));
        
        verifyCommand(new LogoutClientCommand());
    }

}
