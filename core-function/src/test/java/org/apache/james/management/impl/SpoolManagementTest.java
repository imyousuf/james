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



package org.apache.james.management.impl;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.management.SpoolFilter;
import org.apache.james.management.SpoolManagementException;
import org.apache.james.management.impl.SpoolManagement;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.james.InMemorySpoolRepository;
import org.apache.mailet.base.test.MockMimeMessage;
import org.apache.mailet.base.test.MockMail;
import org.apache.mailet.base.test.MailUtil;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.util.HashMap;

import junit.framework.TestCase;

/**
 * Tests the SpoolManagement
 */
public class SpoolManagementTest extends TestCase {

    protected static final String OUTGOING_SPOOL = "file://target/var/mail/outgoing";

    private SpoolManagement m_spoolManagement;
    private MockStore m_mockStore;
    private InMemorySpoolRepository m_mockSpoolRepository;

    protected void setUp() throws Exception {
        m_spoolManagement = new SpoolManagement();
        ContainerUtil.enableLogging(m_spoolManagement, new MockLogger());
        ContainerUtil.service(m_spoolManagement, setUpServiceManager());
    }

    protected void tearDown() throws Exception {
        if (m_mockSpoolRepository != null) {
            ContainerUtil.dispose(m_mockSpoolRepository);
        }
        super.tearDown();
    }

    private MockServiceManager setUpServiceManager() {
        MockServiceManager serviceManager = new MockServiceManager();
        m_mockStore = new MockStore();
        serviceManager.put(Store.ROLE, m_mockStore);
        m_mockSpoolRepository = new InMemorySpoolRepository();
        m_mockStore.add("outgoing", m_mockSpoolRepository);
        return serviceManager;
    }

    public void testListSpoolItemsDontFilter() throws SpoolManagementException, MessagingException {
        String[] mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, SpoolFilter.ERRORMAIL_FILTER);
        assertEquals("emtpy spool", 0, mailList.length); // no mail in spool

        createSpoolMail(Mail.DEFAULT, "subj1", 1);
        createSpoolMail(Mail.ERROR, "subj2", 2);

        mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, new SpoolFilter(null));
        assertEquals("finds all mails", 2, mailList.length);
    }

    public void testListSpoolItemsFilterByState() throws SpoolManagementException, MessagingException {
        MockMail mockMail1 = createSpoolMail(Mail.DEFAULT, "subj1", 1);
        String messageID1 = mockMail1.getName();

        String[] mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, SpoolFilter.ERRORMAIL_FILTER);
        assertEquals("no error mail in spool", 0, mailList.length);

        MockMail mockMail2 = createSpoolMail(Mail.ERROR, "subj2", 2);
        String messageID2 = mockMail2.getName();

        mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, SpoolFilter.ERRORMAIL_FILTER);
        assertEquals("finds only 1 error mail", 1, mailList.length);
        assertTrue("finds only error mail: "+mailList[0], mailList[0].indexOf(messageID2) >= 0);

        mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, new SpoolFilter(Mail.DEFAULT));
        assertEquals("finds only 1 default mail", 1, mailList.length);
        assertTrue("finds only default mail", mailList[0].indexOf(messageID1) >= 0);

        mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, new SpoolFilter(Mail.DEFAULT.toUpperCase()));
        assertEquals("finds default mail uppercase state", 1, mailList.length);
    }

    public void testListSpoolItemsFilterByHeader() throws SpoolManagementException, MessagingException {

        MockMail mockMail1 = createSpoolMail(Mail.DEFAULT, "TestHeader", "value1", 1);
        String messageID1 = mockMail1.getName();

        createSpoolMail(Mail.ERROR, "TestHeader", "value2", 2);
        createSpoolMail(Mail.ERROR, "TestHeader", "another", 3);

        String[] mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, new SpoolFilter("TestHeader", "value[12]"));
        assertEquals("find all mails", 2, mailList.length);

        mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, new SpoolFilter("TestHeader", "(.*)1"));
        assertEquals("find first mail", 1, mailList.length);
        assertTrue("finds only default mail", mailList[0].indexOf(messageID1) >= 0);

        HashMap headerMap = new HashMap();
        headerMap.put("TestHeader", "(.*)1");
        headerMap.put("to", "test2@james.apache.org");
        SpoolFilter matchableFilterWith2Headers = new SpoolFilter(null, headerMap);
        mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, matchableFilterWith2Headers);
        assertEquals("find one mail", 1, mailList.length);
        assertTrue("finds only default mail", mailList[0].indexOf(messageID1) >= 0);

        headerMap = new HashMap();
        headerMap.put("TestHeader", "(.*)1");
        headerMap.put("UNmatchABLE", "test2@james.apache.org");
        SpoolFilter unmatchableFilterWith2Headers = new SpoolFilter(null, headerMap);
        mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, unmatchableFilterWith2Headers);
        assertEquals("find no mail", 0, mailList.length);

    }

    public void testRemoveErrorMails() throws SpoolManagementException, MessagingException {
        createSpoolMail(Mail.DEFAULT, "subj1", 1);
        createSpoolMail(Mail.ERROR, "subj2", 2);
        createSpoolMail(Mail.ERROR, "subj3", 3);

        assertEquals("one mail removed", 2, m_spoolManagement.removeSpoolItems(OUTGOING_SPOOL, null, SpoolFilter.ERRORMAIL_FILTER));
        String[] mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, SpoolFilter.ERRORMAIL_FILTER);
        assertEquals("finds only left non-error mail", 0, mailList.length);
    }

    public void testRemoveErrorDedicatedMail() throws SpoolManagementException, MessagingException {
        createSpoolMail(Mail.DEFAULT, "subj1", 1);
        MockMail mockMail2 = createSpoolMail(Mail.ERROR, "subj2", 2);
        String messageID2 = mockMail2.getName();
        MockMail mockMail3 = createSpoolMail(Mail.ERROR, "subj3", 3);
        String messageID3 = mockMail3.getName();

        assertEquals("one mail removed", 1, m_spoolManagement.removeSpoolItems(OUTGOING_SPOOL, messageID3, SpoolFilter.ERRORMAIL_FILTER));
        String[] mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL, SpoolFilter.ERRORMAIL_FILTER);
        assertEquals("still finds 1 error mail", 1, mailList.length);
        assertTrue("still finds error mail", mailList[0].indexOf(messageID2) >= 0);
    }

    private MockMail createSpoolMail(String state, String subject, int number) throws MessagingException {
        MockMimeMessage mimeMessage = MailUtil.createMimeMessage(subject, number);
        MockMail mockMail = MailUtil.createMockMail2Recipients(mimeMessage);
        mockMail.setState(state);
        m_mockSpoolRepository.store(mockMail);
        return mockMail;
    }

    private MockMail createSpoolMail(String state, String headerName, String headerRegex, int number) throws MessagingException {
        MockMimeMessage mimeMessage = MailUtil.createMimeMessage(headerName, headerRegex, "test", number);
        MockMail mockMail = MailUtil.createMockMail2Recipients(mimeMessage);
        mockMail.setState(state);
        m_mockSpoolRepository.store(mockMail);
        return mockMail;
    }


}
