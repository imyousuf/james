package org.apache.james.mailboxmanager.mailbox;

import org.apache.james.mailboxmanager.MailboxManagerException;

public interface UidMailbox {
	
	
	long getUidValidity() throws MailboxManagerException;
	
	/**
	 * 
	 * @return the uid that will be assigned to the next appended message
	 * @throws MailboxManagerException 
	 */

	long getUidNext() throws MailboxManagerException;

}
