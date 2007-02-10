package org.apache.james.mailboxmanager.acl;

public interface AclManager {
    
    /**
     * 
     * @param mailboxName
     */
    GroupAcl[] getGroupAcls(String mailboxName);
    
    /**
     * 
     * @param mailboxName
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
