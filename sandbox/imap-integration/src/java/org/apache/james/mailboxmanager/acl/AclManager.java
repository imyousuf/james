package org.apache.james.mailboxmanager.acl;

public interface AclManager {
	
	/**
	 * 
	 * @param mailboxName
	 * @param requestingUser to check credentials
	 * @return
	 */
	GroupAcl[] getGroupAcls(String mailboxName);
	
	/**
	 * 
	 * @param mailboxName
	 * @param requestingUser to check credentials
	 * @return
	 */
	UserAcl[] getUserAcls(String mailboxName);
	
	/**
	 * If there are no rights granted/revoked corresponding acl will be removed
	 * 
	 */
	void setGroupAcls(String mailboxName, GroupAcl groupAclacl);
	
	
	/**
	 * If there are no rights granted/revoked corresponding acl will be removed
	 * 
	 */
	void getUserAcls(String mailboxName, UserAcl userAcl);
}
