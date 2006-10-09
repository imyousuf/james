package org.apache.james.mailboxmanager.mailbox;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;



/**
 * 
 * An EventTriggerMailbox will fire an event of the types defined in
 * MailboxListener. When the underlaying store is modified by mupltiple
 * instances it has to keep track of last known status and deliver events as
 * soon as it detects external operations
 * 
 */
public interface EventTriggerMailbox {

	/**
	 * Implementations of Mailbox may interpret the fact that someone is
	 * listening and do some caching and even postpone persistence until
	 * everyone has removed itself.
	 * 
	 * @param listener
	 * @param result
	 *            which fields to be returned in MessageResult
	 * @throws MailboxManagerException 
	 */
	void addListener(MailboxListener listener, int result) throws MailboxManagerException;

	void removeListener(MailboxListener listener);

}
