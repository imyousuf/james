package org.apache.james.mailboxmanager.mailbox;

import java.util.Date;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.mailet.Mail;

/**
 * 
 * This is a step to support mail object spools<br />
 * To fetch Mail objects the normal
 * 
 * <pre>
 * MessageResult[] getMessages(GeneralMessageSet set, int result);
 * </pre>
 * 
 * Method of Mailbox could be used by demanding Mail as an result.
 */

public interface MailMailbox {

    MessageResult appendMail(Mail mail, Date internalDate, int result)
            throws MailboxManagerException;

    MessageResult updateMail(Mail mail, Date internalDate, int result)
            throws MailboxManagerException;
}
