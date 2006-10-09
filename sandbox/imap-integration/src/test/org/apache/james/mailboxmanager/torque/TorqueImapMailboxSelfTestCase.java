package org.apache.james.mailboxmanager.torque;

import javax.mail.MessagingException;

import org.apache.james.mailboxmanager.AbstractImapMailboxSelfTestCase;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.mailboxmanager.mock.MockUser;
import org.apache.james.mailboxmanager.mock.TorqueMailboxManagerProviderSingleton;
import org.apache.torque.TorqueException;

public class TorqueImapMailboxSelfTestCase extends
        AbstractImapMailboxSelfTestCase {
    
    public TorqueImapMailboxSelfTestCase() throws TorqueException {
        super();
    }

    public void setUp() throws Exception {
    	MailboxManagerProvider mailboxManagerProvider=TorqueMailboxManagerProviderSingleton.getTorqueMailboxManagerProviderInstance();
    	mailboxManagerProvider.deleteEverything();
        mailboxManager = mailboxManagerProvider .getGeneralManagerInstance(new MockUser());
        
        super.setUp();
    }
    
    public void testAddedEvents() throws MailboxManagerException, MessagingException {
    	super.testAddedEvents();
    }

}
