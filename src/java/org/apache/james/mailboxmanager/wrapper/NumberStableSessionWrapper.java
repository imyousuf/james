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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.impl.MailboxEventDispatcher;
import org.apache.james.mailboxmanager.impl.MessageResultImpl;
import org.apache.james.mailboxmanager.mailbox.AbstractGeneralMailbox;
import org.apache.james.mailboxmanager.mailbox.EventQueueingSessionMailbox;
import org.apache.james.mailboxmanager.mailbox.EventTriggerMailbox;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;

public abstract class NumberStableSessionWrapper extends AbstractGeneralMailbox implements EventQueueingSessionMailbox,
        MailboxListener {

    protected GeneralMailbox mailbox;

    private UidToMsnBidiMap numberCache = null;
    
    protected Map flagEventMap = new TreeMap();

    protected SortedSet expungedEventList = new TreeSet();

    private MailboxEventDispatcher eventDispatcher = new MailboxEventDispatcher();
    
    public NumberStableSessionWrapper(GeneralMailbox generalMailbox) throws MailboxManagerException {
        this.mailbox=generalMailbox;
        ((EventTriggerMailbox)mailbox).addListener(eventDispatcher, MessageResult.UID);
        getNumberCache();
        eventDispatcher.addMailboxListener(this);
    }

    protected UidToMsnBidiMap getNumberCache() throws MailboxManagerException {
        if (numberCache == null) {
            MessageResult[] mr = mailbox.getMessages(GeneralMessageSetImpl
                    .all(), MessageResult.UID);
            numberCache = new UidToMsnBidiMap();
            for (int i = 0; i < mr.length; i++) {
                numberCache.add(mr[i].getUid());
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
        if (mr != null) {
            if ((result & MessageResult.MSN) > 0) {
                // TODO copy the MessageResult because it could be another class
                int msn = getNumberCache().getMsn(mr.getUid());
                ((MessageResultImpl) mr).setMsn(msn);
            }
        }
        return mr;
    }

    public synchronized MessageResult[] getFlagEvents(boolean reset)
            throws MailboxManagerException {
        final MessageResult[] msnFlagEvents = buildMsnEvents(flagEventMap.values(),false);
        if (reset) {
            flagEventMap = new TreeMap();
        }
        return msnFlagEvents;
    }

    public synchronized MessageResult[] getExpungedEvents(boolean reset)
            throws MailboxManagerException {
        final MessageResult[] msnExpungedEvents  = buildMsnEvents(expungedEventList,reset);
        if (reset) {
            expungedEventList = new TreeSet();
        } 
        return msnExpungedEvents;
    }

    protected MessageResult[]  buildMsnEvents(Collection collection,boolean expunge)
            throws MailboxManagerException {
        final MessageResult[]  msnEvents = new MessageResult[collection.size()];
        int i=0;
        for (Iterator iter = collection.iterator(); iter.hasNext();) {
            MessageResult origMr = (MessageResult) iter.next();
            MessageResultImpl newMr = new MessageResultImpl(origMr);
            newMr.setMsn(getNumberCache().getMsn(origMr.getUid()));
            if (expunge) {
                getNumberCache().expunge(origMr.getUid());
            }
            msnEvents[i++]=newMr;
        }
        return msnEvents;
    }

    public void added(MessageResult result) {
        try {
            getNumberCache().add(result.getUid());
        } catch (MailboxManagerException e) {
            getLog().error("This should not happen",e);
        }
    }

    public void expunged(MessageResult mr) {
        getLog().debug("Expunged: "+mr);
       expungedEventList.add(mr);
    }

    public synchronized void flagsUpdated(MessageResult mr,
            MailboxListener silentListener) {
        if (silentListener != this
                || flagEventMap.containsKey(new Long(mr.getUid()))) {
            // if there has been an external update in the past we should inform
            // about the newest value, even if in silent mode
            flagEventMap.put(new Long(mr.getUid()), mr);
        }
    }

    public void mailboxDeleted() {
        // TODO Auto-generated method stub

    }

    public void mailboxRenamed(String origName, String newName) {
        // TODO Auto-generated method stub

    }
    
    public void addListener(MailboxListener listener, int result) {
        eventDispatcher.addMailboxListener(listener);
    }

    public void removeListener(MailboxListener listener) {
        eventDispatcher.removeMailboxListener(listener);
    }

}
