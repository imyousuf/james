package org.apache.james.mailboxmanager;

/**
 * Provides the existing namespaces 
 */

public interface Namespaces {
    
    Namespace getPersonalDefault();
    Namespace[] getPersonal();
    Namespace[] getShared();
    Namespace[] getUser();

}
