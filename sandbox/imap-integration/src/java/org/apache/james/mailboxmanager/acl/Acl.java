package org.apache.james.mailboxmanager.acl;


/**
 * 
 * order: allow/deny. An ACL will be attached to a mailbox with a belonging
 * group or user. (extended by GroupAcl or UserAcl)
 * 
 */

public interface Acl {

	/**
	 * 
	 * Rights to grant
	 */
	MailboxRights getPositiveRights();

	/**
	 * 
	 * Rights to be revoked. This rights cannot be granted anymore in anyway.
	 */
	MailboxRights getNegativeRights();

}
