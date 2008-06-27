package org.apache.james.mailboxmanager.torque;

import org.apache.james.mailboxmanager.AbstractMailboxManagerSelfTestCase;
import org.apache.james.mailboxmanager.mock.MockUser;
import org.apache.james.mailboxmanager.mock.TorqueMailboxManagerProviderSingleton;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;

public class TorqueMailboxManagerSelfTestCase extends
        AbstractMailboxManagerSelfTestCase {

    public TorqueMailboxManagerSelfTestCase() throws TorqueException {
        super();
    }

    public void setUp() throws Exception {
        TorqueMailboxManagerProviderSingleton.getTorqueMailboxManagerProviderInstance().deleteEverything();
        mailboxManager = TorqueMailboxManagerProviderSingleton.getTorqueMailboxManagerProviderInstance().getGeneralManagerInstance(new MockUser());
    }

}
