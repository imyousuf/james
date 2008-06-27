package org.apache.james.mailboxmanager.torque;

import java.util.List;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.torque.om.MailboxRow;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

public class TorqueMailboxManagerTest extends AbstractMailboxRowTestCase {
    

    
    public TorqueMailboxManagerTest() throws TorqueException {
        super();
    }

    public void testCreateRenameDeleteMailbox() throws TorqueException, MailboxManagerException {
        mm.createMailbox("#users.tuser.INBOX");
        List l=MailboxRowPeer.doSelect(new Criteria());
        assertEquals(1,l.size());
        assertEquals("#users.tuser.INBOX",((MailboxRow)l.get(0)).getName());
        
        mm.renameMailbox("#users.tuser.INBOX","#users.tuser2.INBOX");
        l=MailboxRowPeer.doSelect(new Criteria());
        assertEquals(1,l.size());
        assertEquals("#users.tuser2.INBOX",((MailboxRow)l.get(0)).getName());
        
        mm.deleteMailbox("#users.tuser2.INBOX");
        l=MailboxRowPeer.doSelect(new Criteria());
        assertEquals(0,l.size());
    }
}
