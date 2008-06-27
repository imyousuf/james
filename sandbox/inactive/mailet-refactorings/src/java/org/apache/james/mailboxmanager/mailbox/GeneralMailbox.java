package org.apache.james.mailboxmanager.mailbox;

import java.util.Date;

import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;

public interface GeneralMailbox {
    
    /**
     * 
     * What the Mailbox is able to put out as an result. e.g.: message number, uid, key, MimeMessage, Size, internal Date, flags
     * @see MessageResult
     */
    int getMessageResultTypes();
    
    /**
     * 
     * which queries could be made to select a set of messages, e.g.: range of uid or message numbers, a key, a MimeMessage object
     * @see GeneralMessageSet
     */
    int getMessageSetTypes();
    
    /**
     * Example #mail.paul.lists.apache.james-dev (3rd level folder of user paul)
     * 
     * @return Full folder name with namespace
     * @throws MailboxManagerException 
     */

    String getName() throws MailboxManagerException;

    int getMessageCount() throws MailboxManagerException;
    

    /**
     * @param internalDate
     *            <p>IMAP defines this as the time when the message has arrived to
     *            this server (by smtp). Clients are also allowed to set the
     *            internalDate on apppend.</p><p>Is this Mail.getLastUpdates() for 
     *            James delivery? Should we use MimeMessage.getReceivedDate()?
     * @param result
     *            which fields to be returned in MessageResult
     * @return MessageResult with the fields defined by <b>result</b>
     *         <ul>
     *         <li> IMAP, Javamail Folder: nothing required </li>
     *         <li> UIDPlusFolder: requires to return appended Message or uid</li>
     *         <li> UIDPLUS: requires to return appended uid</li>
     *         </ul>
     * @throws MailboxManagerException
     *             if anything went wrong
     */
    MessageResult appendMessage(MimeMessage message, Date internalDate,
            int result) throws MailboxManagerException;
    
    /**
     * 
     * @param messageSet TODO
     * @param message has to belong to this mailbox and either come as the result from an appendMessage or
     * getMessages operation
     * @param result uid and msn will change TODO should key change, too?
     * @return
     */
    
    MessageResult updateMessage(GeneralMessageSet messageSet, MimeMessage message, int result) throws MailboxManagerException;
    
    /**
     * 
     * @param set
     * @return MessageResult with the fields defined by <b>result</b>
     *         <ul>
     *         <li> IMAP: a set of msn, uid, Flags, header lines, content, mime
     *         parts...</li>
     *         <li> Javamail Folder: Message[]</li>
     *         </ul>
     * @throws MailboxManagerException 
     */

    MessageResult[] getMessages(GeneralMessageSet set, int result) throws MailboxManagerException;


}
