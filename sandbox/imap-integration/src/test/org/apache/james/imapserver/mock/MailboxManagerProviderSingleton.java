package org.apache.james.imapserver.mock;

import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.mailboxmanager.torque.TorqueMailboxManagerProvider;

public class MailboxManagerProviderSingleton {
    
    
    private static TorqueMailboxManagerProvider torqueMailboxManagerProvider;

    public synchronized static MailboxManagerProvider getMailboxManagerProviderInstance() throws Exception {
        if (torqueMailboxManagerProvider==null) {
            torqueMailboxManagerProvider=new TorqueMailboxManagerProvider();
            torqueMailboxManagerProvider.configureDefaults();
            torqueMailboxManagerProvider.initialize();
        }
        return torqueMailboxManagerProvider;
        
    }

}
