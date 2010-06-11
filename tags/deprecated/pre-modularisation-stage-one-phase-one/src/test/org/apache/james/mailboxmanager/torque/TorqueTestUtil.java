package org.apache.james.mailboxmanager.torque;

import org.apache.james.mailboxmanager.TestUtil;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

public class TorqueTestUtil extends TestUtil {

    

    public static void clearTables() throws TorqueException {
//        MessageBodyPeer.doDelete(new Criteria().and(MessageBodyPeer.MAILBOX_ID,
//                new Integer(-1), Criteria.GREATER_THAN));
//        MessageHeaderPeer.doDelete(new Criteria().and(MessageHeaderPeer.MAILBOX_ID,
//                new Integer(-1), Criteria.GREATER_THAN));
//        MessageFlagsPeer.doDelete(new Criteria().and(MessageFlagsPeer.MAILBOX_ID,
//                new Integer(-1), Criteria.GREATER_THAN));
//        MessageRowPeer.doDelete(new Criteria().and(MessageRowPeer.MAILBOX_ID,
//                new Integer(-1), Criteria.GREATER_THAN));
        MailboxRowPeer.doDelete(new Criteria().and(MailboxRowPeer.MAILBOX_ID,
                new Integer(-1), Criteria.GREATER_THAN));
    }

}
