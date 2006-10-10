package org.apache.james.imapserver.mock;

import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.mailboxmanager.mock.TorqueMailboxManagerProviderSingleton;

public class MailboxManagerProviderSingleton {
    
    public synchronized static MailboxManagerProvider getMailboxManagerProviderInstance() throws Exception {
    	return TorqueMailboxManagerProviderSingleton.getTorqueMailboxManagerProviderInstance();
    }

}
