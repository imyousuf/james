/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.                  *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/


package org.apache.james.management;

import junit.framework.TestCase;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.james.MockSpoolRepository;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.james.test.mock.javaxmail.MockMimeMessage;
import org.apache.james.test.util.Util;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

/**
 * Tests the SpoolManagement
 */
public class SpoolManagementTest extends TestCase {

    protected static final String OUTGOING_SPOOL = "file://var/mail/outgoing";

    private SpoolManagement m_spoolManagement;
    private MockStore m_mockStore;
    private MockSpoolRepository m_mockSpoolRepository;

    protected void setUp() throws Exception {
        m_spoolManagement = new SpoolManagement();
        ContainerUtil.enableLogging(m_spoolManagement, new MockLogger());
        ContainerUtil.service(m_spoolManagement, setUpServiceManager());
    }

    private MockServiceManager setUpServiceManager() {
        MockServiceManager serviceManager = new MockServiceManager();
        m_mockStore = new MockStore();
        serviceManager.put(Store.ROLE, m_mockStore);
        m_mockSpoolRepository = new MockSpoolRepository();
        m_mockStore.add("outgoing", m_mockSpoolRepository);
        return serviceManager;
    }

    public void testListSpoolItems() throws SpoolManagementException, MessagingException {
        String[] mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL);
        assertEquals("emtpy spool", 0, mailList.length); // no mail in spool

        MockMail mockMail1 = createSpoolMail(Mail.DEFAULT, "subj1", 1);
        String messageID1 = mockMail1.getMessage().getMessageID();

        mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL);
        assertEquals("no error mail in spool", 0, mailList.length); 

        MockMail mockMail2 = createSpoolMail(Mail.ERROR, "subj2", 2);
        String messageID2 = mockMail2.getMessage().getMessageID();

        mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL);
        assertEquals("finds only 1 error mail", 1, mailList.length);
        assertTrue("finds only error mail", mailList[0].indexOf(messageID2) >= 0);
    }

    public void testRemoveErrorMails() throws SpoolManagementException, MessagingException {
        MockMail mockMail1 = createSpoolMail(Mail.DEFAULT, "subj1", 1);
        String messageID1 = mockMail1.getMessage().getMessageID();

        MockMail mockMail2 = createSpoolMail(Mail.ERROR, "subj2", 2);
        String messageID2 = mockMail2.getMessage().getMessageID();

        MockMail mockMail3 = createSpoolMail(Mail.ERROR, "subj3", 3);
        String messageID3 = mockMail3.getMessage().getMessageID();

        assertEquals("one mail removed", 2, m_spoolManagement.removeSpoolItems(OUTGOING_SPOOL, null));
        String[] mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL);
        assertEquals("finds only left non-error mail", 0, mailList.length);
    }

    public void testRemoveErrorDedicatedMail() throws SpoolManagementException, MessagingException {
        MockMail mockMail1 = createSpoolMail(Mail.DEFAULT, "subj1", 1);
        String messageID1 = mockMail1.getMessage().getMessageID();

        MockMail mockMail2 = createSpoolMail(Mail.ERROR, "subj2", 2);
        String messageID2 = mockMail2.getMessage().getMessageID();

        MockMail mockMail3 = createSpoolMail(Mail.ERROR, "subj3", 3);
        String messageID3 = mockMail3.getMessage().getMessageID();

        assertEquals("one mail removed", 1, m_spoolManagement.removeSpoolItems(OUTGOING_SPOOL, messageID3));
        String[] mailList = m_spoolManagement.listSpoolItems(OUTGOING_SPOOL);
        assertEquals("still finds 1 error mail", 1, mailList.length);
        assertTrue("still finds error mail", mailList[0].indexOf(messageID2) >= 0);
    }

    private MockMail createSpoolMail(String state, String subject, int number) throws MessagingException {
        MockMimeMessage mimeMessage = Util.createMimeMessage(subject, number);
        MockMail mockMail = Util.createMockMail2Recipients(mimeMessage);
        mockMail.setState(state);
        m_mockSpoolRepository.store(mockMail);
        return mockMail;
    }


}
