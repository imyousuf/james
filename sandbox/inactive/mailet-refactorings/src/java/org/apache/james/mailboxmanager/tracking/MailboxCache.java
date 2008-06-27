package org.apache.james.mailboxmanager.tracking;

import java.util.HashMap;
import java.util.Map;


public class MailboxCache {
    
    private Map trackers=new HashMap();

    public synchronized MailboxTracker getMailboxTracker(String mailboxName, Class trackerClass) {
        MailboxTracker tracker=(MailboxTracker) trackers.get(mailboxName);
        if (tracker!=null) {
            if (!tracker.getClass().equals(trackerClass)) {
                tracker.signalDeletion();
                trackers.remove(mailboxName);
                tracker=null;
            }
        }
        return tracker;
    }

    public synchronized void notFound(String mailboxName) {
        MailboxTracker tracker=(MailboxTracker) trackers.get(mailboxName);
        if (tracker!=null) {
            tracker.signalDeletion();
            trackers.remove(mailboxName);
        }
    }

    public synchronized void add(String mailboxName, MailboxTracker tracker) {
        trackers.put(mailboxName,tracker);
    }

    public synchronized void unused(MailboxTracker tracker) {
        trackers.remove(tracker.getMailboxName());
    }

    public synchronized void renamed(String origName, String newName) {
        MailboxTracker tracker=(MailboxTracker) trackers.get(origName);
        if (tracker!=null) {
            // is there already a tracker at the new position??
            notFound(newName);
            trackers.remove(origName);
            trackers.put(newName, tracker);
            tracker.signalRename(newName);
        }
        notFound(origName);
    }

}
