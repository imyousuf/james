package org.apache.james.mailboxmanager;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.impl.MailboxListenerCollector;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.manager.GeneralManager;

public abstract class AbstractImapMailboxSelfTestCase extends TestCase {
    
    protected GeneralManager mailboxManager;
    
    protected ImapMailboxSession mailbox;
    
    protected MailboxListenerCollector collector;
    
    public static final String INBOX="#users.tuser.INBOX";
    
    public void setUp() throws Exception {
        super.setUp();
        mailboxManager.createMailbox(INBOX);
        mailbox=mailboxManager.getGenericImapMailboxSession(INBOX);
        collector=new MailboxListenerCollector();
        mailbox.addListener(collector, 0);
        assertNotNull(mailbox);
    }
    
    public void testGetFirstUnseen() throws MailboxManagerException, MessagingException {
        assertNotNull(mailbox);
        for (int i = 0; i < 5; i++) {
            MimeMessage mm=TestUtil.createMessage();
            mm.setFlags(new Flags(Flags.Flag.SEEN), true);
            MessageResult mr=mailbox.appendMessage(mm, new Date(), MessageResult.UID);
            assertEquals(i+1, mr.getUid());
        }
        for (int i = 0; i < 3; i++) {
            MessageResult mr=mailbox.appendMessage(TestUtil.createMessage(), new Date(), MessageResult.UID);
            assertEquals(i+6, mr.getUid());
        }
        for (int i = 0; i < 3; i++) {
            MimeMessage mm=TestUtil.createMessage();
            mm.setFlags(new Flags(Flags.Flag.SEEN), true);
            MessageResult mr=mailbox.appendMessage(mm, new Date(), MessageResult.UID);
            assertEquals(i+9, mr.getUid());
        }
        MessageResult mr;
        mr=mailbox.getFirstUnseen(MessageResult.UID | MessageResult.MSN);
        assertNotNull(mr);
        assertEquals(6, mr.getUid());
        assertEquals(6, mr.getMsn());
        mailbox.setFlags(new Flags(Flags.Flag.DELETED), true, false, GeneralMessageSetImpl.uidRange(1,3), null);
        mailbox.expunge(GeneralMessageSetImpl.all(), 0);
        mailbox.getExpungedEvents(true);
        mr=mailbox.getFirstUnseen(MessageResult.UID | MessageResult.MSN);
        assertNotNull(mr);
        assertEquals(6, mr.getUid());
        assertEquals(3, mr.getMsn());
    }
    
    public void testGetExpungedEvents() throws MessagingException, MailboxManagerException {
        for (int i = 0; i < 5; i++) {
            MessageResult mr=mailbox.appendMessage(TestUtil.createMessage(), new Date(), MessageResult.UID | MessageResult.MSN);
            assertEquals(i+1, mr.getUid());
            assertEquals(i+1, mr.getMsn());
        }
        mailbox.setFlags(new Flags(Flags.Flag.DELETED), true, false, GeneralMessageSetImpl.uidRange(2, 4), null);
        final MessageResult[] expungeResult1=mailbox.expunge(GeneralMessageSetImpl.all(), MessageResult.UID | MessageResult.MSN);
        checkMessageResults(new long[] {2,3,4},new int[] {2,3,4},expungeResult1);
        
        final MessageResult[] getResult1 = mailbox.getMessages(GeneralMessageSetImpl.all(), MessageResult.UID | MessageResult.MSN);
        checkMessageResults(new long[] {1,5},new int[] {1,5},getResult1);
        
        
        final MessageResult[] expungeEventResult1 = mailbox.getExpungedEvents(false);
        checkMessageResults(new long[] {2,3,4},new int[] {2,3,4},expungeEventResult1);
        final MessageResult[] expungeEventResult2 = mailbox.getExpungedEvents(true);
        checkMessageResults(new long[] {2,3,4},new int[] {2,2,2},expungeEventResult2);
        
        
        final MessageResult[] getResult2 = mailbox.getMessages(GeneralMessageSetImpl.all(), MessageResult.UID | MessageResult.MSN);
        checkMessageResults(new long[] {1,5},new int[] {1,2},getResult2);
        
        for (int i = 0; i < 5; i++) {
            MessageResult mr=mailbox.appendMessage(TestUtil.createMessage(), new Date(), MessageResult.UID | MessageResult.MSN);
            assertEquals(6+i, mr.getUid());
            assertEquals(3+i, mr.getMsn());
        }

        final MessageResult[] getResult3 = mailbox.getMessages(GeneralMessageSetImpl.all(), MessageResult.UID | MessageResult.MSN);
        checkMessageResults(new long[] {1,5,6,7,8,9,10},new int[] {1,2,3,4,5,6,7},getResult3);
        
        mailbox.setFlags(new Flags(Flags.Flag.DELETED), true, false, GeneralMessageSetImpl.msnRange(2,4), null);
        mailbox.setFlags(new Flags(Flags.Flag.DELETED), true, false, GeneralMessageSetImpl.oneMsn(6), null);
        
        final MessageResult[] expungeResult2=mailbox.expunge(GeneralMessageSetImpl.all(), MessageResult.UID | MessageResult.MSN);
        checkMessageResults(new long[] {5,6,7,9},new int[] {2,3,4,6},expungeResult2);
        
        final MessageResult[] getResult4 = mailbox.getMessages(GeneralMessageSetImpl.all(), MessageResult.UID | MessageResult.MSN);
        checkMessageResults(new long[] {1,8,10},new int[] {1,5,7},getResult4);
        
        final MessageResult[] expungeEventResult3 = mailbox.getExpungedEvents(false);
        checkMessageResults(new long[] {5,6,7,9},new int[] {2,3,4,6},expungeEventResult3);
        final MessageResult[] expungeEventResult4 = mailbox.getExpungedEvents(true);
        checkMessageResults(new long[] {5,6,7,9},new int[] {2,2,2,3},expungeEventResult4);
        
        final MessageResult[] getResult5 = mailbox.getMessages(GeneralMessageSetImpl.all(), MessageResult.UID | MessageResult.MSN);
        checkMessageResults(new long[] {1,8,10},new int[] {1,2,3},getResult5);
    }
    
    public void testAddedEvents() throws MailboxManagerException, MessagingException {
        assertEquals(0,mailbox.getFlagEvents(false).length);
        MessageResult mr=mailbox.appendMessage(TestUtil.createMessage(), new Date(), MessageResult.UID | MessageResult.MSN);
        assertEquals(0,mailbox.getFlagEvents(false).length);
        assertEquals(0,mailbox.getFlagEvents(true).length);
    }
    
    protected void assertEventCount(MailboxListenerCollector collector,long[] added, long[] flags, long[] expunged) {
        assertEquals("added size", added.length, collector.getAddedList(true).size());
        assertEquals("flags size", added.length, collector.getFlaggedList(true).size());
        assertEquals("deleted size", added.length, collector.getExpungedList(true).size());
        
    }
    
    protected void checkMessageResults(long[] uids,int[] msns,MessageResult[] messageResults) {
        assertEquals(uids.length, msns.length);
        assertEquals(uids.length, messageResults.length);
        for (int i = 0; i < messageResults.length; i++) {
            assertEquals("Uid at pos "+i,uids[i], messageResults[i].getUid());
            assertEquals("Msn at pos "+i,msns[i], messageResults[i].getMsn());
        }
        
    }

}
