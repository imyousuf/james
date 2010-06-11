package org.apache.james.mailboxmanager.mailbox;


public interface MailboxSession extends Mailbox {

    public void close();
    
    public boolean isWriteable();
    
}
