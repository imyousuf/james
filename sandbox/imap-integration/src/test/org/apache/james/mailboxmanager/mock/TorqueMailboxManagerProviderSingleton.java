package org.apache.james.mailboxmanager.mock;

import org.apache.james.mailboxmanager.torque.TorqueMailboxManagerProvider;

public class TorqueMailboxManagerProviderSingleton {
    
    
    private static TorqueMailboxManagerProvider torqueMailboxManagerProvider;

    public synchronized static TorqueMailboxManagerProvider getTorqueMailboxManagerProviderInstance() throws Exception {
        if (torqueMailboxManagerProvider==null) {
            torqueMailboxManagerProvider=new TorqueMailboxManagerProvider();
            torqueMailboxManagerProvider.configureDefaults();
            torqueMailboxManagerProvider.initialize();
        }
        return torqueMailboxManagerProvider;
        
    }

}
