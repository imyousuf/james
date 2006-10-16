package org.apache.james.mailboxmanager.mock;

import org.apache.james.mailboxmanager.torque.TorqueMailboxManagerProvider;
import org.apache.james.test.mock.james.MockFileSystem;

public class TorqueMailboxManagerProviderSingleton {
    
    
    private static TorqueMailboxManagerProvider torqueMailboxManagerProvider;

    public synchronized static TorqueMailboxManagerProvider getTorqueMailboxManagerProviderInstance() throws Exception {
        if (torqueMailboxManagerProvider==null) {
            torqueMailboxManagerProvider=new TorqueMailboxManagerProvider() {{
                setFileSystem(new MockFileSystem());
            }};
            torqueMailboxManagerProvider.configureDefaults();
            torqueMailboxManagerProvider.initialize();
        }
        return torqueMailboxManagerProvider;
        
    }

}
