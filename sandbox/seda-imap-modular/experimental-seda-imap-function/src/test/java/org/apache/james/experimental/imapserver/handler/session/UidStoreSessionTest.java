package org.apache.james.experimental.imapserver.handler.session;

import java.io.IOException;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.client.LoginCommand;
import org.apache.james.experimental.imapserver.client.LogoutClientCommand;
import org.apache.james.experimental.imapserver.client.MessageSet;
import org.apache.james.experimental.imapserver.client.SelectCommand;
import org.apache.james.experimental.imapserver.client.StoreClientCommand;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.experimental.imapserver.util.MessageGenerator;
import org.apache.james.mailboxmanager.MailboxManagerException;



public class UidStoreSessionTest extends AbstractSessionTest {
    
    
    String[] onlyInbox = {USER_MAILBOX_ROOT+".INBOX"};
    MimeMessage[] msgs= null;
    long[] uids = null;
    
    public void setUp() throws MailboxException, MessagingException, IOException, MailboxManagerException {
        super.setUp();
        msgs=MessageGenerator.generateSimpleMessages(4);
        createFolders(onlyInbox);
        // increase the uid
        appendMessagesClosed(USER_MAILBOX_ROOT+".INBOX",msgs);
        deleteAll(USER_MAILBOX_ROOT+".INBOX");
        uids=addUIDMessagesOpen(USER_MAILBOX_ROOT+".INBOX",msgs);
    }
    
    public void doTestSetFlags(boolean uid, boolean silent) throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        Flags flagsMany= new Flags();
        flagsMany.add(Flag.SEEN);
        flagsMany.add(Flag.FLAGGED);
        flagsMany.add(Flag.ANSWERED);
        setFlags(USER_MAILBOX_ROOT+".INBOX", 5, 6, flagsMany, true, false);
        setFlags(USER_MAILBOX_ROOT+".INBOX", 8, 8, flagsMany, true, false);
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        uids=getUids(USER_MAILBOX_ROOT+".INBOX");
        
        verifyCommand(new SelectCommand(USER_MAILBOX_ROOT+".INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));



        MessageSet set;
        StoreClientCommand storeCommand;

        
        Flags flagsSeen=new Flags();
        flagsSeen.add(Flag.SEEN);
        
        
        if (uid) {
            set=new MessageSet(msgs,uids,7,8);  
        } else {
            set=new MessageSet(msgs,3,4);
        }
        
        msgs[2].setFlag(Flag.SEEN, true);
        msgs[3].setFlags(flagsMany,false);
        msgs[3].setFlag(Flag.SEEN, true);
             
        storeCommand=new StoreClientCommand(StoreClientCommand.MODE_SET,silent,flagsSeen,set);
        verifyCommand(storeCommand);
        
        if (uid) {
            set=new MessageSet(msgs,uids,5);    
        }else {
            set=new MessageSet(msgs,1);
        }
        
        msgs[0].setFlags(flagsMany,false);
        msgs[0].setFlag(Flag.SEEN, true);
        
        storeCommand=new StoreClientCommand(StoreClientCommand.MODE_SET,silent,flagsSeen,set);
        verifyCommand(storeCommand);
        
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        
        assertEquals(flagsSeen,msgs[0].getFlags());
        assertEquals(flagsMany,msgs[1].getFlags());
        assertEquals(flagsSeen,msgs[2].getFlags());
        assertEquals(flagsSeen,msgs[3].getFlags());
        verifyCommand(new LogoutClientCommand());
        assertFalse(isOpen(USER_MAILBOX_ROOT+".INBOX"));
    }
    
    public void doTestAddFlags(boolean uid, boolean silent) throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        setFlags(USER_MAILBOX_ROOT+".INBOX", 5, 8, new Flags(Flags.Flag.DRAFT), true, false);
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand(USER_MAILBOX_ROOT+".INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        uids=getUids(USER_MAILBOX_ROOT+".INBOX");

        MessageSet set;
        StoreClientCommand storeCommand;
        
        Flags flagsSeen=new Flags();
        flagsSeen.add(Flag.SEEN);
        if (uid) {
            set=new MessageSet(msgs,uids,5);    
        } else {
            set=new MessageSet(msgs,1);
        }
        msgs[0].setFlags(flagsSeen, true);
        
        storeCommand=new StoreClientCommand(StoreClientCommand.MODE_ADD,silent,flagsSeen,set);
        verifyCommand(storeCommand);
        
        Flags flagsMany= new Flags();
        flagsMany.add(Flag.SEEN);
        flagsMany.add(Flag.FLAGGED);
        flagsMany.add(Flag.ANSWERED);
        
        if (uid) {
            set=new MessageSet(msgs,uids,6,8);  
        }else {
            set=new MessageSet(msgs,2,4);
        }
        msgs[1].setFlags(flagsMany, true);
        msgs[2].setFlags(flagsMany, true);
        msgs[3].setFlags(flagsMany, true);

        storeCommand=new StoreClientCommand(StoreClientCommand.MODE_ADD,silent,flagsMany,set);
        verifyCommand(storeCommand);
        
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        
        flagsSeen.add(Flags.Flag.DRAFT);
        flagsMany.add(Flags.Flag.DRAFT);
        
        assertEquals(flagsSeen,msgs[0].getFlags());
        assertEquals(flagsMany,msgs[1].getFlags());
        assertEquals(flagsMany,msgs[2].getFlags());
        assertEquals(flagsMany,msgs[3].getFlags());
        verifyCommand(new LogoutClientCommand());
        assertFalse(isOpen(USER_MAILBOX_ROOT+".INBOX"));
    }
    
    public void doTestRemoveFlags(boolean uid, boolean silent) throws ProtocolException, IOException, MessagingException, MailboxManagerException {

        Flags flagsEmpty=new Flags();
        
        Flags flagsSeen=new Flags();
        flagsSeen.add(Flag.SEEN);
        
        Flags flagsAnswered=new Flags();
        flagsAnswered.add(Flag.ANSWERED);
        
        Flags flagsSeenFlagged= new Flags();
        flagsSeenFlagged.add(Flag.SEEN);
        flagsSeenFlagged.add(Flag.FLAGGED);
        
        Flags flagsSeenFlaggedAnswered= new Flags();
        flagsSeenFlaggedAnswered.add(Flag.SEEN);
        flagsSeenFlaggedAnswered.add(Flag.FLAGGED);
        flagsSeenFlaggedAnswered.add(Flag.ANSWERED);
        
        setFlags(USER_MAILBOX_ROOT+".INBOX", 5, 5, flagsSeenFlaggedAnswered, true, false);
        setFlags(USER_MAILBOX_ROOT+".INBOX", 6, 6, flagsSeen, true, false);
        setFlags(USER_MAILBOX_ROOT+".INBOX", 7, 8, flagsSeenFlaggedAnswered, true, false);

        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        uids=getUids(USER_MAILBOX_ROOT+".INBOX");
        
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand(USER_MAILBOX_ROOT+".INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));
        
        MessageSet set;
        StoreClientCommand storeCommand;

        set=new MessageSet(msgs,1,3,new int[] {1,3},uids,uid);
        
        msgs[0].setFlag(Flags.Flag.ANSWERED,false);
        msgs[2].setFlag(Flags.Flag.ANSWERED,false);
       
        storeCommand=new StoreClientCommand(StoreClientCommand.MODE_REMOVE,silent,flagsAnswered,set);
        verifyCommand(storeCommand);

        if (uid) {
            set=new MessageSet(msgs,uids,8);
        } else {
            set=new MessageSet(msgs,4);
        }
        msgs[3].setFlags(flagsSeenFlaggedAnswered,false);
        
        storeCommand=new StoreClientCommand(StoreClientCommand.MODE_REMOVE,silent,flagsSeenFlaggedAnswered,set);
        verifyCommand(storeCommand);
        
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        
        assertEquals(flagsSeenFlagged,msgs[0].getFlags());
        assertEquals(flagsSeen,msgs[1].getFlags());
        assertEquals(flagsSeenFlagged,msgs[2].getFlags());
        assertEquals(flagsEmpty,msgs[3].getFlags());
        verifyCommand(new LogoutClientCommand());
        assertFalse(isOpen(USER_MAILBOX_ROOT+".INBOX"));
    }
    
    public void testSetFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestSetFlags(false, false);
    }
    
    public void testAddFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestAddFlags(false, false);
    }
    
    public void testRemoveFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestRemoveFlags(false, false);
    }
    
    public void testUidSetFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestSetFlags(true, false);
    }
    
    public void testUidAddFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestAddFlags(true, false);
    }
    
    public void testUidRemoveFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestRemoveFlags(true, false);
    }
    
    
    public void testSilentSetFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestSetFlags(false, true);
    }
    
    public void testSilentAddFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestAddFlags(false, true);
    }
    
    public void testSilentRemoveFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestRemoveFlags(false, true);
    }
    
    public void testSilentUidSetFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestSetFlags(true, true);
    }
    
    public void testSilentUidAddFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestAddFlags(true, true);
    }
    
    public void testSilentUidRemoveFlags() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        doTestRemoveFlags(true, true);
    }
}
