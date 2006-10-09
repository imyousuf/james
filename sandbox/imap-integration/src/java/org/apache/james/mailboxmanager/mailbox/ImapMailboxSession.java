package org.apache.james.mailboxmanager.mailbox;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.Quota;
import org.apache.james.mailboxmanager.acl.MailboxRights;


/**
 * This is the Mailbox from the view of the user<br />
 * 
 * <p>Not sure whether it should extend ImapMailbox or provide it with a getMailbox() method.</p>
 * <p>If it extends ImapMailbox it requires an adapter but it would be possible to check rights
 * and maybe quota on access.</p> 
 * <p>Another requirements for sessions is to keep the message numbers stable. Maybe message numbers
 * should only be provided by the session</p>
 */

public interface ImapMailboxSession extends ImapMailbox, MailboxListener, GeneralMailboxSession, EventQueueingSessionMailbox {
	
	
	/**
	 *
	 * @return the effective rights to this mailbox
	 */
	MailboxRights myRights();
	/**
	 * 
	 * @return the quota that is assigned to this mailbox
	 */
	Quota[] getQuota();

	boolean isSelectable();
    
}
