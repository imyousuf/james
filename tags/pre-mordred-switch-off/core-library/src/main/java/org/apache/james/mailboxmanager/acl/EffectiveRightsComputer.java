package org.apache.james.mailboxmanager.acl;


/**
 * 
 * Used to compute the effective rights of a bunch of Acl objects bound to a
 * mailbox. For a specific user there could be one user acl and multiple group
 * acls that take effect. The proposed approach is allow/deny. This means first
 * sum up all rights granted by group membership or user acl, then remove every
 * right that is revoked.<br />
 * Another approach could be that a right granted by a user acl cannot be
 * revoked by a group acl.
 * 
 */

public interface EffectiveRightsComputer {

    public void setUserAcl(UserAcl userAcl);

    public void setGroupAcls(GroupAcl[] groupAcl);

    /**
     * used to filter the groups to retain only the ones the user is member of.<br 7>
     * TODO just a draft...
     * 
     * @param groupMemberships
     */
    public void retainGroups(String[] groupMemberships);

    public MailboxRights computeEffective();

}
