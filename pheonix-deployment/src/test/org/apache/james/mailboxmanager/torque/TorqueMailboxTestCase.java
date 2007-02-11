package org.apache.james.mailboxmanager.torque;

import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.TestUtil;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.impl.MailboxListenerCollector;
import org.apache.james.mailboxmanager.torque.om.MailboxRow;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.james.mailboxmanager.torque.om.MessageRow;
import org.apache.james.mailboxmanager.torque.om.MessageRowPeer;
import org.apache.james.mailboxmanager.tracking.UidChangeTracker;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

public class TorqueMailboxTestCase extends AbstractTorqueTestCase {


    public TorqueMailboxTestCase() throws TorqueException {
        super();
    }

    public void testAppendGetDeleteMessage() throws Exception {
        MailboxRow mr = new MailboxRow("#users.tuser.INBOX", 100);
        mr.save();
        mr=MailboxRowPeer.retrieveByName("#users.tuser.INBOX");
        TorqueMailbox torqueMailbox = new TorqueMailbox(mr, new UidChangeTracker(null,"#users.tuser.INBOX",100),null);
        torqueMailbox.addListener(new MailboxListenerCollector(), MessageResult.NOTHING);
        assertEquals(0,torqueMailbox.getMessageCount());
        
        long time = System.currentTimeMillis();
        time = time - (time % 1000);
        Date date = new Date(time);
        MimeMessage mm=TestUtil.createMessage();
        Flags flags=new Flags();
        flags.add(Flags.Flag.ANSWERED);
        flags.add(Flags.Flag.SEEN);
        mm.setFlags(flags,true);
        mm.writeTo(System.out);
        torqueMailbox.appendMessage(mm, date, 0);
        assertEquals(1,torqueMailbox.getMessageCount());
        List l = MessageRowPeer.doSelect(new Criteria());
        assertEquals(1, l.size());
        MessageRow msg = (MessageRow) l.get(0);
        assertEquals(mr.getMailboxId(), msg.getMailboxId());
        assertEquals(1, msg.getUid());

        assertEquals(date, msg.getInternalDate());
        assertEquals(flags, msg.getMessageFlags().getFlagsObject());

        mr = MailboxRowPeer.retrieveByPK(mr.getMailboxId());
        assertEquals(1, mr.getLastUid());
        
        MessageResult[] messageResult=torqueMailbox.getMessages(GeneralMessageSetImpl.oneUid(1l),MessageResult.MIME_MESSAGE);
        assertNotNull(messageResult);
        assertEquals(1,messageResult.length);
        messageResult[0].getMimeMessage().writeTo(System.out);
        assertTrue(TorqueTestUtil.contentEquals(mm,messageResult[0].getMimeMessage(),true));
        
        Flags f=new Flags();
        f.add(Flags.Flag.DELETED);
        torqueMailbox.setFlags(f,true,false, GeneralMessageSetImpl.oneUid(1l), null);
        MessageResult[] messageResults=torqueMailbox.expunge(GeneralMessageSetImpl.all(),MessageResult.UID);
        assertEquals(1,messageResults.length);
        assertEquals(1l,messageResults[0].getUid());
    }

}