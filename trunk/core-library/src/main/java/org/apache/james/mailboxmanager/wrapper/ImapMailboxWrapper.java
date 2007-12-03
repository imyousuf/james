/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailboxmanager.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.SearchParameters;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.impl.MailboxEventDispatcher;
import org.apache.james.mailboxmanager.impl.MessageResultImpl;
import org.apache.james.mailboxmanager.mailbox.AbstractGeneralMailbox;
import org.apache.james.mailboxmanager.mailbox.EventQueueingSessionMailbox;
import org.apache.james.mailboxmanager.mailbox.FlaggedMailbox;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.mailbox.SearchableMailbox;

public class ImapMailboxWrapper extends AbstractGeneralMailbox 
        implements ImapMailboxSession, EventQueueingSessionMailbox, MailboxListener  {


    private static final class MsnIterator implements Iterator {
            private final Iterator it;
            private final UidToMsnBidiMap map;
            
            public MsnIterator(final Iterator it, final UidToMsnBidiMap map) {
                this.it = it;
                this.map = map;
            }
            
            public boolean hasNext() {
                return it.hasNext();
            }
    
            public Object next() {
                final MessageResult next = (MessageResult) it.next();
                final int msn = map.getMsn(next.getUid());
                final MessageResult result = new MsnMessageResult(next, msn);
                return result;
            }
    
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
            private static final class MsnMessageResult implements MessageResult {
                private final MessageResult delegate;
                private final int msn;
                
                public MsnMessageResult(final MessageResult delegate, final int msn) {
                    super();
                    this.delegate = delegate;
                    this.msn = msn;
                }

                public Flags getFlags() throws MailboxManagerException {
                    return delegate.getFlags();
                }

                public Content getFullMessage() throws MailboxManagerException {
                    return delegate.getFullMessage();
                }

                public int getIncludedResults() {
                    return delegate.getIncludedResults() | MessageResult.MSN;
                }

                public Date getInternalDate() {
                    return delegate.getInternalDate();
                }

                public String getKey() {
                    return delegate.getKey();
                }

                public Content getMessageBody() throws MailboxManagerException {
                    return delegate.getMessageBody();
                }

                public MimeMessage getMimeMessage() throws MailboxManagerException {
                    return delegate.getMimeMessage();
                }

                public int getMsn() {
                    return msn;
                }

                public int getSize() {
                    return delegate.getSize();
                }

                public long getUid() {
                    return delegate.getUid();
                }

                public long getUidValidity() {
                    return delegate.getUidValidity();
                }

                public Iterator iterateHeaders() throws MailboxManagerException {
                    return delegate.iterateHeaders();
                }

                public int compareTo(Object o) {
                    return delegate.compareTo(o);
                }
                
            }
        }

    protected GeneralMailbox mailbox;

    private UidToMsnBidiMap numberCache = null;
    
    protected Map flagEventMap = new TreeMap();

    protected SortedSet expungedEventList = new TreeSet();

    private MailboxEventDispatcher eventDispatcher = new MailboxEventDispatcher();
    
    private boolean startingNumberCache = false;
    
    
    public ImapMailboxWrapper(ImapMailbox mailbox) throws MailboxManagerException {
        setMailbox(mailbox);
        init();
    }
    
    public void setMailbox(ImapMailbox mailbox) {
        this.mailbox=mailbox;
    }

    public void init() throws MailboxManagerException {
        mailbox.addListener(eventDispatcher);
        eventDispatcher.addMailboxListener(this);        
    }

    protected UidToMsnBidiMap getNumberCache() throws MailboxManagerException {
        if (numberCache == null && mailbox != null) {
            startingNumberCache = true;
            try {
                final Iterator it = mailbox.getMessages(GeneralMessageSetImpl
                        .all(), MessageResult.UID);
                numberCache = new UidToMsnBidiMap();
                while (it.hasNext()) {
                    final MessageResult result = (MessageResult) it.next();
                    final long uid = result.getUid();
                    numberCache.add(uid);
                }
            } finally {
                startingNumberCache = false;
            }
        }
        return numberCache;
    }

    protected GeneralMessageSet toUidSet(GeneralMessageSet set)
            throws MailboxManagerException {

        if (set.getType() == GeneralMessageSet.TYPE_MSN) {
            set = GeneralMessageSetImpl.uidRange(getNumberCache().getUid(
                    set.getMsnFrom()), getNumberCache().getUid(set.getMsnTo()));
        }
        return set;
    }

    protected static int noMsnResult(int result) {
        if ((result & MessageResult.MSN) > 0) {
            result |= MessageResult.UID;
            result -= MessageResult.MSN;
        }
        return result;
    }

    protected MessageResult[] addMsnToResults(MessageResult[] mr, int result)
            throws MailboxManagerException {
        MessageResult[] translated = new MessageResult[mr.length];
        for (int i = 0; i < translated.length; i++) {
            translated[i] = addMsnResult(mr[i], result);
        }
        return translated;
    }

    protected MessageResult addMsnResult(MessageResult mr, int result)
            throws MailboxManagerException {
        // added events dispatched before the cache has been started
        // should be ignored
        if (mr != null) {
            if ((result & MessageResult.MSN) > 0) {
                // TODO copy the MessageResult because it could be another class
                int msn = getNumberCache().getMsn(mr.getUid());
                ((MessageResultImpl) mr).setMsn(msn);
            }
        }
        return mr;
    }

    public synchronized Iterator getExpungedEvents(boolean reset)
            throws MailboxManagerException {
        final Collection msnExpungedEvents  = buildMsnEvents(expungedEventList,reset);
        if (reset) {
            expungedEventList = new TreeSet();
        } 
        return msnExpungedEvents.iterator();
    }

    private Collection  buildMsnEvents(final Collection messageResults, 
            final boolean expunge)
            throws MailboxManagerException {
        final Collection msnEvents = new ArrayList(messageResults.size());
        for (final Iterator iter = messageResults.iterator(); iter.hasNext();) {
            final Long uidObject = (Long) iter.next();
            final long uid = uidObject.longValue();
            final MessageResultImpl message = new MessageResultImpl(uid);
            final UidToMsnBidiMap numberCache = getNumberCache();
            final int msn = numberCache.getMsn(uid);
            message.setMsn(msn);
            if (expunge) {
                numberCache.expunge(uid);
            }
            msnEvents.add(message);
        }
        return msnEvents;
    }

    public void added(final long uid) {
        try {
            // added events dispatched before the cache has been started
            // should be ignored
            if (!startingNumberCache && numberCache != null) 
            {
                getNumberCache().add(uid);
            }
        } catch (MailboxManagerException e) {
            getLog().error("This should not happen",e);
        }
    }

    public void expunged(final long uid) {
        getLog().debug("Expunged: "+uid);
        expungedEventList.add(new Long(uid));
    }

    public synchronized void flagsUpdated(final long uid, final Flags flags, long sessionId) {
        final Long uidObject = new Long(uid);
        if (sessionId != getSessionId() && !flagEventMap.containsKey(uidObject)) {
            // if there has been an external update in the past we should inform
            // about the newest value, even if in silent mode
            
            // only store flags
            final MessageResultImpl lightweightResult = new MessageResultImpl(uid);
            lightweightResult.setFlags(flags);
            flagEventMap.put(uidObject, lightweightResult);
        }
    }

    
    
    /**
     * @see org.apache.james.mailboxmanager.MailboxListener#event(org.apache.james.mailboxmanager.MailboxListener.Event)
     */
    public void event(Event event) {
        if (event instanceof MessageEvent) {
            final long sessionId = event.getSessionId();
            final MessageEvent messageEvent = (MessageEvent) event;
            final long uid = messageEvent.getSubjectUid();
            if (event instanceof Added) {
                added(uid);
            } else if (event instanceof Expunged) {
                expunged(uid);
            } else if (event instanceof FlagsUpdated) {
                final FlagsUpdated flagsUpdated = (FlagsUpdated) event;
                flagsUpdated(flagsUpdated.getSubjectUid(), flagsUpdated.getNewFlags(), 
                        sessionId);
            }
        }
    }
    
    public void addListener(MailboxListener listener) {
        eventDispatcher.addMailboxListener(listener);
    }

    public void removeListener(MailboxListener listener) {
        eventDispatcher.removeMailboxListener(listener);
    }
    
    public void close() {
        mailbox.removeListener(eventDispatcher);
        mailbox=null;
    }
    
    protected final Iterator addMsn(Iterator iterator) throws MailboxManagerException {
        return new MsnIterator(iterator, getNumberCache());
    }

    /**
     * for testing
     * @return the listener this class uses to subscribe to Mailbox events
     */
    
    MailboxListener getListenerObject() {
        return eventDispatcher;
    }
    

    public synchronized Iterator expunge(GeneralMessageSet set, int result) throws MailboxManagerException {
        final GeneralMessageSet uidSet = toUidSet(set);
        final int noMsnResult = noMsnResult(result);
        final Iterator expunge = ((FlaggedMailbox) mailbox).expunge(uidSet, noMsnResult);
        return addMsn(expunge);
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

    public Iterator setFlags(Flags flags, boolean value, boolean replace, GeneralMessageSet set, int result) throws MailboxManagerException {
        final Iterator results = ((FlaggedMailbox) mailbox).setFlags(flags, value, replace,toUidSet(set), result);
        return addMsn(results);
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

    public Iterator getMessages(GeneralMessageSet set, int result)
            throws MailboxManagerException {
        return addMsn(mailbox.getMessages(toUidSet(set),
                noMsnResult(result)));
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

    public boolean isWriteable() {
        return true;
    }

    public void remove(GeneralMessageSet set) throws MailboxManagerException {
        mailbox.remove(toUidSet(set));
    }

    public long getSessionId() {
        return mailbox.getSessionId();
    }

    public long getUidValidity() throws MailboxManagerException {
        return ((ImapMailbox) mailbox).getUidValidity();
    }

    public long getUidNext() throws MailboxManagerException {
        return ((ImapMailbox) mailbox).getUidNext();
    }

    public Iterator search(GeneralMessageSet set, SearchParameters searchTerm, int result) throws MailboxManagerException {
        final Iterator results = ((SearchableMailbox)mailbox).search(set, searchTerm, noMsnResult(result));
        return addMsn(results);
    }

}
