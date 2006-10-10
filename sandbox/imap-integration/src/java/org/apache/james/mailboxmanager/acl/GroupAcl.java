package org.apache.james.mailboxmanager.acl;


/**
 * 
 * Group acl bound to a mailbox. 
 *
 */

public interface GroupAcl extends Acl {
    
    /**
     * 
     * could be enviroment specific. e.g. a unix group or a ldap dn
     */
    
    public String getGroupName();

}
