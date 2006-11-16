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

package org.apache.james.mailboxmanager.mailstore;

import java.util.Arrays;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.services.MailRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.constraint.IsEqual;

public class MailStoreMailboxCacheTest extends MockObjectTestCase {

    MailstoreMailboxCache mailstoreMailboxCache;

    public void setUp() {
        mailstoreMailboxCache = new MailstoreMailboxCache();
        mailstoreMailboxCache.enableLogging(new MockLogger());
        mailstoreMailboxCache.setMountPoint("#mail");
        mailstoreMailboxCache.setDestinationURL("file://var/mail/inboxes/");
    }

    public void testBuildUrlNoInbox() {
        assertEquals("file://var/mail/inboxes/t1", mailstoreMailboxCache
                .buildUrl("#mail.t1"));
        assertEquals("file://var/mail/inboxes/t1.t2", mailstoreMailboxCache
                .buildUrl("#mail.t1.t2"));
    }

    public void testBuildUrlInbox() {
        assertEquals("file://var/mail/inboxes/t1", mailstoreMailboxCache
                .buildUrl("#mail.t1.INBOX"));
        assertEquals("file://var/mail/inboxes/t1.t2", mailstoreMailboxCache
                .buildUrl("#mail.t1.t2.INBOX"));
    }

    public void testGetMailboxSession() throws MailboxManagerException,
            ConfigurationException {
        
        DefaultConfiguration repositoryConfiguration = new DefaultConfiguration(
                "repository");

        DefaultConfiguration expectedConfiguration = new DefaultConfiguration(
                repositoryConfiguration);
        expectedConfiguration.setAttribute("destinationURL",
                "file://var/mail/inboxes/t1");

        Mock store = mock(Store.class);
        Mock mockRepository = mock(MailRepository.class);
        store.expects(exactly(2)).method("select").with(
                new IsEqual(expectedConfiguration)).will(
                returnValue(mockRepository.proxy()));

        mailstoreMailboxCache.setMailStore((Store) store.proxy());
        mailstoreMailboxCache.setRepositoryConf(repositoryConfiguration);
        MailboxSession s1 = mailstoreMailboxCache.getMailboxSession("#mail.t1.INBOX");
        MailboxSession s2 = mailstoreMailboxCache.getMailboxSession("#mail.t1.INBOX");
        assertNotSame(s1, s2);
        s1.close();
        MailboxSession s3 = mailstoreMailboxCache.getMailboxSession("#mail.t1.INBOX");
        s3.close();
        s2.close();

        MailboxSession s4 = mailstoreMailboxCache.getMailboxSession("#mail.t1.INBOX");

        String[] keys = new String[] { "test" };
        mockRepository.expects(once()).method("list").withNoArguments().will(
                returnIterator(keys));
        assertEquals(Arrays.asList(keys), s4.list());
    }

}
