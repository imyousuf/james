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

package org.apache.james.mailboxmanager;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.apache.commons.collections.IteratorUtils;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.impl.MailboxListenerCollector;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.manager.MailboxManager;

public abstract class AbstractImapMailboxSelfTestCase extends TestCase {
    
    protected MailboxManager mailboxManager;
    
    public MailboxSession mailboxSession;
    
    protected ImapMailbox mailbox;
    
    protected MailboxListenerCollector collector;
    
    public static final String INBOX="#users.tuser.INBOX";
    
    public void setUp() throws Exception {
        super.setUp();
        mailboxManager.createMailbox(INBOX);
        mailbox=mailboxManager.getImapMailbox(INBOX, false);
        mailboxSession = mailboxManager.createSession();
        collector=new MailboxListenerCollector();
        mailbox.addListener(collector);
        assertNotNull(mailbox);
    }
    
    public void testGetFirstUnseen() throws MailboxManagerException, MessagingException {
        assertNotNull(mailbox);
        for (int i = 0; i < 5; i++) {
            MimeMessage mm=TestUtil.createMessage();
            mm.setFlags(new Flags(Flags.Flag.SEEN), true);
            MessageResult mr=mailbox.appendMessage(mm, new Date(), MessageResult.MINIMAL, mailboxSession);
            assertEquals(i+1, mr.getUid());
        }
        for (int i = 0; i < 3; i++) {
            MessageResult mr=mailbox.appendMessage(TestUtil.createMessage(), new Date(), MessageResult.MINIMAL, mailboxSession);
            assertEquals(i+6, mr.getUid());
        }
        for (int i = 0; i < 3; i++) {
            MimeMessage mm=TestUtil.createMessage();
            mm.setFlags(new Flags(Flags.Flag.SEEN), true);
            MessageResult mr=mailbox.appendMessage(mm, new Date(), MessageResult.MINIMAL, mailboxSession);
            assertEquals(i+9, mr.getUid());
        }
        MessageResult mr;
        mr=mailbox.getFirstUnseen(MessageResult.MINIMAL, mailboxSession);
        assertNotNull(mr);
        assertEquals(6, mr.getUid());
        mailbox.setFlags(new Flags(Flags.Flag.DELETED), true, false, GeneralMessageSetImpl.uidRange(1,3), MessageResult.MINIMAL, mailboxSession);
        mailbox.expunge(GeneralMessageSetImpl.all(), 0, mailboxSession);
        mr=mailbox.getFirstUnseen(MessageResult.MINIMAL, mailboxSession);
        assertNotNull(mr);
        assertEquals(6, mr.getUid());
    }
    
    
    public void testGetUidNext() throws MessagingException {
        assertEquals(1, mailbox.getUidNext(mailboxSession));
        MessageResult mr=mailbox.appendMessage(TestUtil.createMessage(), new Date(), MessageResult.MINIMAL, mailboxSession);
        assertEquals(1,mr.getUid());
        assertEquals(2, mailbox.getUidNext(mailboxSession));
    }
    
    protected void assertEventCount(MailboxListenerCollector collector,long[] added, long[] flags, long[] expunged) {
        assertEquals("added size", added.length, collector.getAddedList(true).size());
        assertEquals("flags size", added.length, collector.getFlaggedList(true).size());
        assertEquals("deleted size", added.length, collector.getExpungedList(true).size());
        
    }
    
    protected void checkMessageResults(long[] uids,int[] msns, Iterator messages) {
        List messageResults = IteratorUtils.toList(messages);
        assertEquals(uids.length, msns.length);
        assertEquals(uids.length, messageResults.size());
        int i=0;
        for (Iterator it=messageResults.iterator(); it.hasNext();i++) {
            assertEquals("Uid at pos "+i,uids[i], ((MessageResult)it.next()).getUid());
        }
    }

}
