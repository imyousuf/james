package org.apache.james.experimental.imapserver;

import java.util.HashMap;
import java.util.Map;

import org.apache.james.api.imap.ImapSessionState;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.SelectedMailboxSession;
import org.apache.james.experimental.imapserver.encode.ImapResponseComposer;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;

public class MockImapSession implements ImapSession {

    public String clientHostName = "localhost";
    public String clientIP = "127.0.0.1";
    
    public Map attributes = new HashMap();
    
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

    public void setAuthenticated(User user) {

    }

    public void setSelected(ImapMailboxSession mailbox, boolean readOnly)
            throws MailboxManagerException {
    }

    public void unsolicitedResponses(ImapResponseComposer response, boolean useUid) {
    }

    public void unsolicitedResponses(ImapResponseComposer request,
            boolean omitExpunged, boolean useUid) {
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
}
