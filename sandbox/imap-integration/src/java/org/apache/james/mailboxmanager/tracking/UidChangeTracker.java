package org.apache.james.mailboxmanager.tracking;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.mail.Flags;

import org.apache.james.mailboxmanager.Constants;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.MessageResultImpl;

/**
 * 
 * 
 * @author Joachim
 * 
 */
public class UidChangeTracker extends MailboxTracker implements Constants {

    private TreeMap cache = new TreeMap();

    private long lastUidAtStart;

    private long lastUid;

    private long lastScannedUid = 0;

    public UidChangeTracker(MailboxCache mailboxCache, String mailboxName,
            long lastUid) {
        super(mailboxCache, mailboxName);
        this.lastUidAtStart = lastUid;
        this.lastUid = lastUid;
    }

    public synchronized void expunged(MessageResult[] expunged) {
        for (int i = 0; i < expunged.length; i++) {
            if (expunged[i] != null) {
                cache.remove(new Long(expunged[i].getUid()));
                eventDispatcher.expunged(expunged[i]);
            }
        }
    }

    public synchronized void found(UidRange range,
            MessageResult[] messageResults, MailboxListener silentListener) {
        Set expectedSet = getSubSet(range);
        for (int i = 0; i < messageResults.length; i++) {
            if (messageResults[i] != null) {
                long uid = messageResults[i].getUid();
                if (uid>lastScannedUid) {
                    lastScannedUid=uid;
                }
                if (expectedSet.contains(new Long(uid))) {
                    expectedSet.remove(new Long(uid));
                    if (messageResults[i].getFlags() != null) {
                        Flags cachedFlags = (Flags) cache.get(new Long(uid));
                        if (cachedFlags == null
                                || !messageResults[i].getFlags().equals(
                                        cachedFlags)) {
                            eventDispatcher.flagsUpdated(messageResults[i],
                                    silentListener);
                            cache.put(new Long(uid), messageResults[i].getFlags());
                        }
                    }
                } else {
                    cache.put(new Long(uid), messageResults[i].getFlags());
                    if (uid > lastUidAtStart) {
                        eventDispatcher.added(messageResults[i]);
                    }
                }
            }

        }

        if (lastScannedUid>lastUid) {
            lastUid=lastScannedUid;
        }
        if (range.getToUid()==UID_INFINITY || range.getToUid()>=lastUid) {
            lastScannedUid=lastUid;
        } else if (range.getToUid()!=UID_INFINITY && range.getToUid()<lastUid && range.getToUid() > lastScannedUid) {
            lastScannedUid=range.getToUid();
        }
        

        
        for (Iterator iter = expectedSet.iterator(); iter.hasNext();) {
            long uid = ((Long) iter.next()).longValue();

            MessageResultImpl mr = new MessageResultImpl();
            mr.setUid(uid);
            eventDispatcher.expunged(mr);
        }
    }

    private SortedSet getSubSet(UidRange range) {
        if (range.getToUid() > 0) {
            return new TreeSet(cache.subMap(new Long(range.getFromUid()),
                    new Long(range.getToUid() + 1)).keySet());
        } else {
            return new TreeSet(cache
                    .tailMap(new Long(range.getFromUid())).keySet());
        }

    }

    public synchronized void found(MessageResult messageResult,
            MailboxListener silentListener) {
        if (messageResult != null) {
            long uid = messageResult.getUid();
            found(new UidRange(uid, uid),
                    new MessageResult[] { messageResult }, silentListener);
        }
    }

    public synchronized long getLastUid() {
        return lastUid;
    }
    
    public synchronized void foundLastUid(long foundLastUid) {
        if (foundLastUid>lastUid) {
            lastUid=foundLastUid;
        }
    }

    public synchronized long getLastScannedUid() {
        return lastScannedUid;
    }



}
