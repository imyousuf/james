package org.apache.james.mailboxmanager.manager;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.services.User;

public interface MailboxManagerProvider {
    
    public MailboxManager getMailboxManagerInstance(User user, Class neededClass) throws MailboxManagerException;
    
    public GeneralManager getGeneralManagerInstance(User user) throws MailboxManagerException;
    
    public void deleteEverything() throws MailboxManagerException;

}
