package org.apache.james.mailboxmanager.manager;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.mailet.User;

public interface MailboxManagerProvider {
    
    public MailboxManager getMailboxManagerInstance(User user, Class neededClass) throws MailboxManagerException;
    
    public GeneralManager getGeneralManagerInstance(User user) throws MailboxManagerException;
    
    public void deleteEverything() throws MailboxManagerException;

}
