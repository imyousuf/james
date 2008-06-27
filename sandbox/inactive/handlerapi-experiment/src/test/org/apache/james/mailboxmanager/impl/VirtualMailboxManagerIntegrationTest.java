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

package org.apache.james.mailboxmanager.impl;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.core.AvalonMailStore;
import org.apache.james.imapserver.util.MessageGenerator;
import org.apache.james.mailboxmanager.TestUtil;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.services.FileSystem;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.james.MockFileSystem;
import org.apache.james.userrepository.DefaultJamesUser;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.xml.sax.SAXException;

public class VirtualMailboxManagerIntegrationTest extends MockObjectTestCase {

    Configuration confFile;

    public void testMixedMailstores() throws Exception {
        Mock mockService = mock(ServiceManager.class);
        mockService.expects(exactly(2)).method("lookup").with(eq(Store.ROLE))
                .will(returnValue(getMailStore()));

        DefaultMailboxManagerProvider mailboxManagerProvider = new DefaultMailboxManagerProvider();
        mailboxManagerProvider.enableLogging(new MockLogger());
        mailboxManagerProvider.configure(getConfFile().getChild(
                "mailboxmanager", false));
        mailboxManagerProvider.service((ServiceManager) mockService.proxy());
        mailboxManagerProvider.initialize();

        MailboxSession session;

        session = mailboxManagerProvider.getInboxSession(new DefaultJamesUser(
                "user1", "none"));
        doMiniTestOnSession(session);
        session = mailboxManagerProvider.getInboxSession(new DefaultJamesUser(
                "user2", "none"));
        doMiniTestOnSession(session);
    }

    protected void doMiniTestOnSession(MailboxSession session)
            throws MessagingException, IOException {
        MimeMessage origMessage = MessageGenerator.generateSimpleMessage();
        String key = session.store(origMessage);
        System.out.println("Key: " + key);
        assertNotNull(key);
        assertEquals(key, session.list().iterator().next());

        MimeMessage fetchedMessage = session.retrieve(key);
        assertTrue(TestUtil.contentEquals(origMessage, fetchedMessage, true));

        session.remove(key);

        assertEquals(0, session.list().size());
        session.close();
    }

    protected Store getMailStore() throws Exception {
        Mock mockService = mock(ServiceManager.class);
        mockService.expects(atLeastOnce()).method("lookup").with(
                eq(FileSystem.ROLE)).will(returnValue(new MockFileSystem()));

        AvalonMailStore mailStore = new AvalonMailStore();
        mailStore.enableLogging(new MockLogger());
        mailStore.configure(getConfFile().getChild("mailstore", false));
        mailStore.service((ServiceManager) mockService.proxy());
        mailStore.initialize();
        return mailStore;
    }

    protected Configuration getConfFile() throws ConfigurationException,
            SAXException, IOException {
        if (confFile == null) {
            confFile = new DefaultConfigurationBuilder()
                    .build(getClass()
                            .getResourceAsStream(
                                    "/org/apache/james/mailboxmanager/testdata/MixedMailstores.xml"));
        }
        return confFile;
    }

}
