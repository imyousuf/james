package org.apache.james.imapserver;

import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;

public class MockImapSession implements ImapSession {

    public String clientHostName = "localhost";
    public String clientIP = "127.0.0.1";
    
    public String buildFullName(String mailboxName)
            throws MailboxManagerException {
        return mailboxName;
    }

    public void closeConnection() {
    }

    public void closeConnection(String byeMessage) {
    }

    public void closeMailbox() throws MailboxManagerException {
    }

    public void deselect() {
    }

    public String getClientHostname() {
        return clientHostName;
    }

    public String getClientIP() {
        return clientIP;
    }

    public MailboxManager getMailboxManager() throws MailboxManagerException {
//      TODO: mock 
        return null;
    }

    public SelectedMailboxSession getSelected() {
//      TODO: mock 
        return null;
    }

    public ImapSessionState getState() {
//      TODO: mock 
        return null;
    }

    public User getUser() {
//      TODO: mock 
        return null;
    }

    public UsersRepository getUsers() {
        // TODO: mock 
        return null;
    }

    public void setAuthenticated(User user) {

    }

    public void setSelected(ImapMailboxSession mailbox, boolean readOnly)
            throws MailboxManagerException {
    }

    public void unsolicitedResponses(ImapResponse response, boolean useUid)
            throws MailboxException {
    }

    public void unsolicitedResponses(ImapResponse request,
            boolean omitExpunged, boolean useUid) throws MailboxException {
    }
}
