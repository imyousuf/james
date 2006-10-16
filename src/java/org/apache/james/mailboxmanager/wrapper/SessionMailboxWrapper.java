package org.apache.james.mailboxmanager.wrapper;

import java.util.Date;

import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.mailbox.FlaggedMailbox;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;

public class SessionMailboxWrapper extends NumberStableSessionWrapper implements MailboxSession {

    public SessionMailboxWrapper(GeneralMailbox generalMailbox) throws MailboxManagerException {
        super(generalMailbox);
    }

    public MessageResult appendMessage(MimeMessage message, Date internalDate,
            int result) throws MailboxManagerException {
        return addMsnResult(mailbox.appendMessage(message, internalDate, noMsnResult(result)),result);
    }

    public int getMessageCount() throws MailboxManagerException {
        return mailbox.getMessageCount();
    }

    public int getMessageResultTypes() {
        return mailbox.getMessageResultTypes() | MessageResult.MSN;
    }

    public MessageResult[] getMessages(GeneralMessageSet set, int result)
            throws MailboxManagerException {
        return addMsnToResults(mailbox.getMessages(toUidSet(set),
                noMsnResult(result)), result);
    }

    public int getMessageSetTypes() {
        return mailbox.getMessageSetTypes() | GeneralMessageSet.TYPE_MSN;
    }

    public String getName() throws MailboxManagerException {
        return mailbox.getName();
    }

    public MessageResult updateMessage(GeneralMessageSet messageSet, MimeMessage message, int result) throws MailboxManagerException {
        return addMsnResult(mailbox.updateMessage(toUidSet(messageSet), message, noMsnResult(result)),result);
    }

    public void close() {
        // TODO Auto-generated method stub
    }

    public boolean isWriteable() {
        return true;
    }

}
