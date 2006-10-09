package org.apache.james.mailboxmanager;



/**
 * Quota for a list of mailboxes.
 * <h4>Milestone 5</h4>
 * <p>
 * Quotas are not treated hierarchically but have to be set for each mailbox
 * individually. When a mailbox is created or renamed, it has to inherit the
 * quota of its parent folder. An instantiated Quota object has to be setup
 * with the accessing user for further credential checks.
 * </p>
 * <p>
 * Implementation of quota management could of course be done in a hierarchy
 * way.
 * </p>
 * 
 * <p>
 * I came to the conclusion that it makes no sense to have user specific quotas.
 * <p>
 * 
 */
public interface Quota {

	/**
	 * <p>
	 * The root mailbox for this quota. All mailboxes that are assigned to this
	 * quota must be childs of the root mailbox or the root mailbox itself. If
	 * the quota spans several namespaces (#news., #mail.,#shared.) root mailbox
	 * can be an empty string (""). That means it is bound to the namespace root.
	 * </p>
	 * <p>
	 * Users are limited to create quotas with a root mailbox they have the
	 * rights to manage quota for. Administrators should use a suitable root
	 * mailbox where ever possible for better clarity.
	 * </p>
	 * <p>
	 * When the root mailbox is deleted the assigned quotas will be deleted too.
	 * If the root mailbox gets moved to another parent quota is deleted too,
	 * because the moved mailboxes have to inherit the quota of its new parents.
	 * </p>
	 * 
	 * @return root mailbox
	 */

	String getRootMailbox();

	/**
	 * 
	 * @param rootMailbox
	 *            the root mailbox
	 */
	void setRootMailbox(String rootMailbox);

	/**
	 * <p>Indicates that the user needs quota management rights to the parent mailbox of root
	 * mailbox to be able to manage this quota</p>
	 * <p>Example: A user should be able to setup and manage quotas in his own mailboxes. But he
	 * should not be able to modify/delete the quota setup by his admin. NeedsParentRights would be 
	 * set true and the admin would need to have the quota management right for "#mail"</p>
	 * <p>If needsParentRights is false this could be understood like a self-control quota</p>
	 *
	 * @return true, if user needs quota management rights to parent folder to manage this quota
	 */
	
	boolean getNeedsParentRights();
	
	/**
	 * 
	 * @param needsParentRights true, if user needs quota management rights to parent folder to manage this quota
	 */
	void setNeedsParentRights(boolean needsParentRights);
	
	/**
	 * <p>
	 * The name of this quota which could be free chosen. The name has to be
	 * unique for the root mailbox. Some implementation specific names may be
	 * reserved like "user_mailbox_quota" that is setup by the administrator for every
	 * user.
	 * </p>
	 * <p>
	 * From the imap view the name is a combination of root mailbox and name
	 * separated by a semi colon ";". For example user joe sets up a quota for
	 * his trash folder: "#mail.joe.trash;my_trash_quota"
	 * </p>
	 * 
	 * @return the name
	 */

	String getName();

	/**
	 * List mailboxes that are assigned to this quota.
	 * 
	 * @param base
	 * @param expression
	 * @return
	 */

	ListResult[] list(String base, String expression);

	/**
	 * Add a Mailbox to this quota. (This is not performed hierarchically) You
	 * can only add mailboxes that belong to the same repository.
	 * 
	 * @param name
	 *            mailbox name
	 * @param user
	 *            to check credentials
	 * @throws IllegalArgumentException
	 *             if the mailbox does not belong to this store TODO throw
	 *             another exception
	 */

	void addMailbox(String name);

	/**
	 * Removes a Mailbox from this quota. (This is not performed hierarchically)
	 * 
	 * @param name
	 *            mailbox name
	 * @param user
	 *            to check credentials
	 */

	void removeMailbox(String name);

	/**
	 * The total sum of messages in all mailboxes that belong to this quota
	 * 
	 * @return message count
	 */

	int getMessageCount();

	/**
	 * The limit for getMessageCount()
	 * 
	 * @return maximal message count or -1 if there is no limit
	 */

	int getMessageCountLimit();

	/**
	 * 
	 * @param limit
	 *            maximal message count or -1 if there is no limit
	 */
	void setMessageCountLimit(int limit);

	/**
	 * The total sum of storage usage in all mailboxes that belong to this quota
	 * in KiB (1024 bytes).
	 * 
	 * @return storage usage
	 */

	int getStorageUsage();

	/**
	 * The limit for getStorageUsage()
	 * 
	 * @return maximal storage usage or -1 if there is no limit
	 */
	int getStorageUsageLimit();

	/**
	 * 
	 * @param limit
	 *            maximal storage usage or -1 if there is no limit
	 */
	void setStorageUsageLimit(int limit);

	/**
	 * <p>
	 * The maximal allowed age (internal date) of messages in all mailboxes that
	 * belong to this quota in days.
	 * </p>
	 * <p>
	 * This will be interesting for mailing lists or news groups. Another
	 * possibility is to automaticly move old messages into an archive by the
	 * purger
	 * </p>
	 * 
	 * @return maximal age in days or -1 if there is no limit
	 */
	int getAgeLimit();

	/**
	 * 
	 * @param days
	 *            maximal age in days or -1 if there is no limit
	 */

	void setAgeLimit(int days);

	/**
	 * <p>
	 * The purger that will come into action when the quota is exceeded in any
	 * way. Implementations may decide whether to trigger purging at once or at
	 * specific intervals or even manually.
	 * </p>
	 * 
	 * @return the purger, null if there is no automatic purging
	 */

	Purger getPurger();

	/**
	 * 
	 * @param purger
	 *            the purger, null if there is no automatic purging
	 */
	void setPurger(Purger purger);

	/**
	 * Presists the changes. Implementations may decide to persist changes at
	 * once.
	 */

	void save();
}
