package org.apache.james.mailboxmanager.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MessageResult;

public class MailboxEventDispatcher implements MailboxListener {

    private Set listeners = new HashSet();

    public void addMailboxListener(MailboxListener mailboxListener) {
        listeners.add(mailboxListener);
    }

    public void removeMailboxListener(MailboxListener mailboxListener) {
        listeners.remove(mailboxListener);
    }

    public void added(MessageResult result) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			MailboxListener mailboxListener = (MailboxListener) iter.next();
			mailboxListener.added(result);
		}
    }

    public void expunged(MessageResult mr) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			MailboxListener mailboxListener = (MailboxListener) iter.next();
            mailboxListener.expunged(mr);
        }
    }

    public void flagsUpdated(MessageResult result, MailboxListener silentListener) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			MailboxListener mailboxListener = (MailboxListener) iter.next();
            mailboxListener.flagsUpdated(result, silentListener);
        }
    }

    public void mailboxDeleted() {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			MailboxListener mailboxListener = (MailboxListener) iter.next();
            mailboxListener.mailboxDeleted();
        }
    }

    public void mailboxRenamed(String origName, String newName) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			MailboxListener mailboxListener = (MailboxListener) iter.next();
            mailboxListener.mailboxRenamed(origName,origName);
        }
    }
    
    public int size() {
        return listeners.size();
    }

}
