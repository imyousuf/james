package org.apache.james.mailboxmanager.acl;

import org.apache.james.services.User;

public interface GroupAwareUser extends User {
    /**
     * could be enviroment specific. e.g. a unix group or a ldap dn
     */
    String[] getGroups();

}
