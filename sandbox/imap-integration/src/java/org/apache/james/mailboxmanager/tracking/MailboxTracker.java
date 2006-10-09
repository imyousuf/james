package org.apache.james.mailboxmanager.tracking;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.impl.MailboxEventDispatcher;

public class MailboxTracker {
    
    protected String mailboxName;
    
    private MailboxCache mailboxCache;
    
    protected MailboxEventDispatcher eventDispatcher=new MailboxEventDispatcher();
    
	private boolean existing;

    public MailboxTracker(MailboxCache mailboxCache, String mailboxName) {
        this.mailboxName=mailboxName;
        this.mailboxCache=mailboxCache;
    }

    public String getMailboxName() {
        return mailboxName;
    }

    public void signalDeletion() {
        eventDispatcher.mailboxDeleted();
        existing=false;
    }
    
    public void addMailboxListener(MailboxListener subject) {
        eventDispatcher.addMailboxListener(subject);
    }
    
    public void removeMailboxListener(MailboxListener subject) {
        eventDispatcher.removeMailboxListener(subject); 
        if (eventDispatcher.size()==0) {
            mailboxCache.unused(this);
        }
    }
    
	public void mailboxNotFound() {
		mailboxCache.notFound(getMailboxName());
		existing=false;
	}

	public boolean isExisting() {
		return existing;
	}

	public void signalRename(String newName) {
        eventDispatcher.mailboxRenamed(mailboxName, newName);
        mailboxName=newName;
	}

}
