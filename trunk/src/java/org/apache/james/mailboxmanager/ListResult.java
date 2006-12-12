package org.apache.james.mailboxmanager;

/**
 * Returned by the list method of MailboxRepository and others
 */

public interface ListResult {
    
    /**
     * \Noinferiors, \Noselect, \Marked, \Unmarked TODO this should be done in a different way..
     */
    String[] getAttributes();
    
    String getHierarchyDelimiter();
    
    /**
     * @return full namespace-name
     */
    
    String getName();

}
