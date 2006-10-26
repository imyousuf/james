package org.apache.james.mailboxmanager.torque;

import org.apache.james.mailboxmanager.manager.GeneralManager;
import org.apache.james.mailboxmanager.mock.MockUser;
import org.apache.james.mailboxmanager.mock.TorqueMailboxManagerProviderSingleton;
import org.apache.james.mailboxmanager.torque.om.MailboxRowPeer;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

public abstract class AbstractMailboxRowTestCase extends AbstractTorqueTestCase {

    GeneralManager mm;
    
    public AbstractMailboxRowTestCase() throws TorqueException {
        super();
    }
    public void setUp() throws Exception {
        super.setUp();
        assertEquals(0,MailboxRowPeer.doSelect(new Criteria()).size());
        mm=TorqueMailboxManagerProviderSingleton.getTorqueMailboxManagerProviderInstance().getGeneralManagerInstance(new MockUser());
    }
    public void tearDown() {
        
    }
    
}
