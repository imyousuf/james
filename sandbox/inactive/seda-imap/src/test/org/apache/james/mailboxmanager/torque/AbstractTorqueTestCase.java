package org.apache.james.mailboxmanager.torque;

import junit.framework.TestCase;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mock.TorqueMailboxManagerProviderSingleton;
import org.apache.torque.TorqueException;

public abstract class AbstractTorqueTestCase extends TestCase {
    
    public AbstractTorqueTestCase() throws TorqueException {
        super();
    }

    public void setUp() throws TorqueException, MailboxManagerException, Exception {
        TorqueMailboxManagerProviderSingleton.getTorqueMailboxManagerProviderInstance().deleteEverything();
    }
}
