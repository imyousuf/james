package org.apache.james.mailboxmanager.wrapper;

import javax.mail.search.SearchTerm;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.Quota;
import org.apache.james.mailboxmanager.acl.MailboxRights;
import org.apache.james.mailboxmanager.impl.MailboxEventDispatcher;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;

public class ImapMailboxSessionWrapper extends FlaggedSessionMailboxWrapper
        implements ImapMailboxSession {

    protected MailboxEventDispatcher eventDispatcher = new MailboxEventDispatcher();

    public ImapMailboxSessionWrapper(ImapMailbox imapMailbox) throws MailboxManagerException {
        super(imapMailbox);
    }

    public MailboxRights myRights() {
        // TODO Auto-generated method stub
        return null;
    }

    public Quota[] getQuota() {
        // TODO Auto-generated method stub
        return null;
    }



    public boolean isSelectable() {
        // TODO Auto-generated method stub
        return true;
    }

    public long getUidValidity() throws MailboxManagerException {
        return ((ImapMailbox) mailbox).getUidValidity();
    }

    public long getUidNext() throws MailboxManagerException {
        return ((ImapMailbox) mailbox).getUidNext();
    }

    public MessageResult[] search(GeneralMessageSet set, SearchTerm searchTerm, int result) {
        // TODO Auto-generated method stub
        return null;
    }





}
