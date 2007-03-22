package org.apache.james.mailboxmanager.wrapper;

import javax.mail.Flags;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.mailbox.FlaggedMailbox;

public class FlaggedSessionMailboxWrapper extends SessionMailboxWrapper {
    
    public FlaggedSessionMailboxWrapper(FlaggedMailbox flaggedMailbox) throws MailboxManagerException {
        super(flaggedMailbox);
    }
    
    
    public synchronized MessageResult[] expunge(GeneralMessageSet set, int result) throws MailboxManagerException {
        return addMsnToResults(((FlaggedMailbox) mailbox).expunge(toUidSet(set), noMsnResult(result)),result);
    }

    public MessageResult getFirstUnseen(int result) throws MailboxManagerException {
        return addMsnResult(((FlaggedMailbox) mailbox).getFirstUnseen(noMsnResult(result)),result);
    }

    public Flags getPermanentFlags() {
        return ((FlaggedMailbox) mailbox).getPermanentFlags();
    }

    public int getRecentCount(boolean reset) throws MailboxManagerException {
        return ((FlaggedMailbox) mailbox).getRecentCount(reset);
    }

    public int getUnseenCount() throws MailboxManagerException {
        return ((FlaggedMailbox) mailbox).getUnseenCount();
    }

    public void setFlags(Flags flags, boolean value, boolean replace, GeneralMessageSet set, MailboxListener silentListener) throws MailboxManagerException {
        ((FlaggedMailbox) mailbox).setFlags(flags, value, replace,toUidSet(set), silentListener);
    }
    

}
