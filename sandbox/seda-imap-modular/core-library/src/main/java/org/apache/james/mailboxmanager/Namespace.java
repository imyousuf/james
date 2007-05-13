package org.apache.james.mailboxmanager;

/**
 * A namespace consists of the name and a hierarchy delimiter (e.g "." or "/")
 */

public interface Namespace {
    
    String getName();
    String getHierarchyDelimter();

}
