package org.apache.james.mailboxmanager.mailbox;

import java.util.List;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;

public interface EventQueueingSessionMailbox {
	
	MessageResult[] getFlagEvents(boolean reset) throws MailboxManagerException;
	
    MessageResult[] getExpungedEvents(boolean reset) throws MailboxManagerException;

}
