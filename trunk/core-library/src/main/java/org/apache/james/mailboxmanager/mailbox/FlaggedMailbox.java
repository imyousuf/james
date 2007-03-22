package org.apache.james.mailboxmanager.mailbox;

import javax.mail.Flags;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;

public interface FlaggedMailbox extends GeneralMailbox {
    
    /**
     * @return Flags that can be stored
     */

    Flags getPermanentFlags();
    

    int getRecentCount(boolean reset) throws MailboxManagerException;
    
    int getUnseenCount() throws MailboxManagerException;
    
    MessageResult getFirstUnseen(int result) throws MailboxManagerException;
    
    /**
     * 
     * @param set
     *            <ul>
     *            <li> IMAP, Javamail: not required, always expunge all</li>
     *            <li> UIDPLUS: requires the possibility of defining a uid range</li>
     *            </ul>
     * 
     * @param result
     *            which fields to be returned in MessageResult
     * @return MessageResult with the fields defined by <b>result</b><br />
     *         <ul>
     *         <li> IMAP, UIDPLUS: nothing required </li>
     *         <li> Javamail Folder: requires the expunged Message[]</li>
     *         </ul>
     * @throws MailboxManagerException
     *             if anything went wrong
     */
    MessageResult[] expunge(GeneralMessageSet set, int result)
            throws MailboxManagerException;
    

    /**
     * this is much more straight forward for IMAP instead of setting Flags of
     * an array of lazy-loading MimeMessages. <br />
     * required by IMAP
     * 
     * @param flags
     *            Flags to be set
     * @param value
     *            true = set, false = unset
     * @param replace
     *            replace all Flags with this flags, value has to be true
     * @param set
     *            the range of messages
     * @param silentListener
     *            IMAP requires the ability of setting Flags without getting an
     *            acknowledge TODO this may be handled only inside of a session
     * @throws MailboxManagerException
     */

    void setFlags(Flags flags, boolean value, boolean replace, GeneralMessageSet set,
            MailboxListener silentListener) throws MailboxManagerException;

}
