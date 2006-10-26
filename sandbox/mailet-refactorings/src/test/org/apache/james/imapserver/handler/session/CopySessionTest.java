package org.apache.james.imapserver.handler.session;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.client.CopyClientCommand;
import org.apache.james.imapserver.client.LoginCommand;
import org.apache.james.imapserver.client.LogoutClientCommand;
import org.apache.james.imapserver.client.MessageSet;
import org.apache.james.imapserver.client.SelectCommand;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.util.MessageGenerator;
import org.apache.james.mailboxmanager.MailboxManagerException;

public class CopySessionTest extends AbstractSessionTest {
    
    
    String[] folders = {USER_MAILBOX_ROOT+".INBOX",USER_MAILBOX_ROOT+".test",USER_MAILBOX_ROOT+".test2"};
    MimeMessage[] msgs= null;
    long[] uids = null;
    
    public void setUp() throws MailboxException, MessagingException, IOException, MailboxManagerException {
        super.setUp();
        msgs=MessageGenerator.generateSimplesMessages(4);
        createFolders(folders);
        // increase the uid
        appendMessagesClosed(USER_MAILBOX_ROOT+".INBOX",msgs);
        deleteAll(USER_MAILBOX_ROOT+".INBOX");
        msgs[0].setFlag(Flag.SEEN,true);
        msgs[0].setFlag(Flag.ANSWERED,true);
        msgs[1].setFlag(Flag.SEEN,true);
        msgs[2].setFlag(Flag.ANSWERED,true);
        uids=addUIDMessagesOpen(USER_MAILBOX_ROOT+".INBOX",msgs);
    }
    
    
    public void doTestCopyAll(boolean uid) throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));

        final MessageSet set;
        if (uid) {
            set = new MessageSet(msgs,uids,1,-1);
        } else {
            set = new MessageSet(msgs,1,-1);
        }
        verifyCommand(new CopyClientCommand(set,"test"));
        
        useFolder(USER_MAILBOX_ROOT+".test");
        MimeMessage[] testMsgs=getMessages(USER_MAILBOX_ROOT+".test");
        assertEquals(4,testMsgs.length);
        for (int i = 0; i < msgs.length; i++) {
            assertEquals("Message content differs "+i, MessageGenerator.messageContentToString(msgs[i]),
                    MessageGenerator.messageContentToString(testMsgs[i]));
            assertEquals("Flags differ "+i,msgs[i].getFlags(),testMsgs[i].getFlags());
        }
        freeFolder(USER_MAILBOX_ROOT+".test");
        
        verifyCommand(new LogoutClientCommand());
        assertFalse(isOpen(USER_MAILBOX_ROOT+".INBOX"));
        assertFalse(isOpen(USER_MAILBOX_ROOT+".test")); 
    }
    
    
    public void doTestCopyOne(boolean uid) throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));

        final MessageSet set;
        if (uid) {
            set = new MessageSet(msgs,uids,6);
        } else {
            set = new MessageSet(msgs,2);
        }
        verifyCommand(new CopyClientCommand(set,"test"));
        
        useFolder("test");
        MimeMessage[] testMsgs=getMessages(USER_MAILBOX_ROOT+".test");
        assertEquals(1,testMsgs.length);
        
            assertEquals("Message content differs ", MessageGenerator.messageContentToString(msgs[1]),
                    MessageGenerator.messageContentToString(testMsgs[0]));
            assertEquals("Flags differ ",msgs[1].getFlags(),testMsgs[0].getFlags());
        
        freeFolder(USER_MAILBOX_ROOT+".test");
        
        verifyCommand(new LogoutClientCommand());
        assertFalse(isOpen(USER_MAILBOX_ROOT+".INBOX"));
        assertFalse(isOpen(USER_MAILBOX_ROOT+".test")); 
    }
    public void doTestCopyThree(boolean uid) throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));

        final MessageSet set;
        if (uid) {
            set = new MessageSet(msgs,uids,6,8);
        } else {
            set = new MessageSet(msgs,2,4);
        }
        verifyCommand(new CopyClientCommand(set,"test"));
        
        useFolder(USER_MAILBOX_ROOT+".test");
        MimeMessage[] testMsgs=getMessages(USER_MAILBOX_ROOT+".test");
        assertEquals(3,testMsgs.length);
        for (int i=0; i<3; i++) {
            assertEquals("Message content differs ", MessageGenerator.messageContentToString(msgs[i+1]),
                    MessageGenerator.messageContentToString(testMsgs[i]));
            assertEquals("Flags differ ",msgs[i+1].getFlags(),testMsgs[i].getFlags());
        }
        
        freeFolder(USER_MAILBOX_ROOT+".test");
        
        verifyCommand(new LogoutClientCommand());
        assertFalse(isOpen(USER_MAILBOX_ROOT+".INBOX"));
        assertFalse(isOpen(USER_MAILBOX_ROOT+".test")); 
    }
    
    
    public void testCopyAll() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestCopyAll(false);
    }
    
    public void testUidCopyAll() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestCopyAll(true);
    }
    
    public void testCopyOne() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestCopyOne(false);
    }
    
    public void testUidCopyOne() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestCopyOne(true);
    }
    
    public void testCopyThree() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestCopyThree(false);
    }
    
    public void testUidCopyThree() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestCopyThree(true);
    }
}
