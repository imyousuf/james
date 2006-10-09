package org.apache.james.mailboxmanager.acl;

import java.util.HashMap;


/**
 * Reprensents a set of rights that can be assigned to a mailbox in combination
 * of an ACL. It is similar to javamails com.sun.mail.imap.Rights
 */

public class MailboxRights {

	public MailboxRights() {

	}

	public void add(Right right) {
		;
	}

	/**
	 * returns a string representation like defined in RFC 2086. Rights not
	 * supported by RFC 2086 will be omitted.
	 * 
	 */
	public String toImapString() {
		return null;

	}


	
	public void remove(Right right) {
		;
	}

	public boolean contains(Right right) {
		return false;
	}
/**
 * 
 * draft an incomplete. Idea is to internally use String representives. The list of possible rights
 * could increase a lot. That would make it impossible to find an appropriate char.
 *
 */
	public static final class Right {

		private static HashMap allRights = new HashMap();

		public static final Right WRITE = getInstance("w");

		private Right(String representative) {

		}

		private synchronized static final Right getInstance(String s) {
			Right right = (Right) allRights.get(s);
			if (right == null) {
				right = new Right(s);
			}
			return right;
		}

	}

}
