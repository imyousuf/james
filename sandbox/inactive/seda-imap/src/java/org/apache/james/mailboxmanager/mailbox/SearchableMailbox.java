package org.apache.james.mailboxmanager.mailbox;

import javax.mail.search.SearchTerm;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;

public interface SearchableMailbox {
    /**
     * @param result
     *            which fields to be returned in MessageResult
     * @return MessageResult with the fields defined by <b>result</b>
     *         <ul>
     *         <li> IMAP: msn or (msn and uid)</li>
     *         <li> Javamail Folder: Message[]</li>
     *         </ul>
     * @throws MailboxManagerException
     *             if anything went wrong
     */
    MessageResult[] search(GeneralMessageSet set,SearchTerm searchTerm, int result) throws MailboxManagerException;
}
