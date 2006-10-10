package org.apache.james.mailboxmanager.manager;

import org.apache.james.mailboxmanager.mailbox.BasicMailboxSession;
import org.apache.james.services.User;

public interface BasicManager {

    /**
     * 
     * @param user
     * @return
     */
    BasicMailboxSession getInbox(User user);
    
    void deleteAllUserData(User user);
}
