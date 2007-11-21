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

package org.apache.james.mailboxmanager.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResultUtils;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

public class MailboxEventDispatcher implements MailboxListener {
    
    private final Set listeners = new CopyOnWriteArraySet();

    public void addMailboxListener(MailboxListener mailboxListener) {
        listeners.add(mailboxListener);
    }

    public void removeMailboxListener(MailboxListener mailboxListener) {
        listeners.remove(mailboxListener);
    }

    public void added(MessageResult result, long sessionId) {
        if (MessageResultUtils.isUidIncluded(result)) {
            final AddedImpl added = new AddedImpl(sessionId, result.getUid());
            for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                MailboxListener mailboxListener = (MailboxListener) iter.next();
                mailboxListener.event(added);
            }
        }
    }

    public void expunged(final MessageResult result, long sessionId) {
        if (MessageResultUtils.isUidIncluded(result)) {
            final long uid = result.getUid();
            final ExpungedImpl expunged = new ExpungedImpl(sessionId, uid);
            for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                MailboxListener mailboxListener = (MailboxListener) iter.next();
                mailboxListener.event(expunged);
            }
        }
    }

    public void flagsUpdated(final MessageResult result, long sessionId, final Flags original,
            final Flags updated) {
        if (MessageResultUtils.isUidIncluded(result)) {
            final long uid = result.getUid();
            final FlagsUpdatedImpl flags = new FlagsUpdatedImpl(sessionId, uid, 
                    original, updated);
            for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                MailboxListener mailboxListener = (MailboxListener) iter.next();
                mailboxListener.event(flags);
            }
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

    public void event(Event event) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            MailboxListener mailboxListener = (MailboxListener) iter.next();
            mailboxListener.event(event);
        }
    }
    
    public int size() {
        return listeners.size();
    }

    private final static class AddedImpl extends MailboxListener.Added {

        private final long sessionId;
        private final long subjectUid;
        
        public AddedImpl(final long sessionId, final long subjectUid) {
            super();
            this.sessionId = sessionId;
            this.subjectUid = subjectUid;
        }

        public long getSubjectUid() {
            return subjectUid;
        }

        public long getSessionId() {
            return sessionId;
        }
    }
    
    private final static class ExpungedImpl extends MailboxListener.Expunged {

        private final long sessionId;
        private final long subjectUid;
        
        public ExpungedImpl(final long sessionId, final long subjectUid) {
            super();
            this.sessionId = sessionId;
            this.subjectUid = subjectUid;
        }

        public long getSubjectUid() {
            return subjectUid;
        }

        public long getSessionId() {
            return sessionId;
        }
    }
    
    private final static class FlagsUpdatedImpl extends MailboxListener.FlagsUpdated {

        private static final boolean isChanged(final Flags original, final Flags updated, 
                Flags.Flag flag) {
            return original != null && updated != null 
                && (original.contains(flag) ^ updated.contains(flag)) ;
        }
        
        private static final Flags.Flag[] FLAGS = {Flags.Flag.ANSWERED,
            Flags.Flag.DELETED, Flags.Flag.DRAFT, Flags.Flag.FLAGGED,
            Flags.Flag.RECENT, Flags.Flag.SEEN};
        private static final int NUMBER_OF_SYSTEM_FLAGS = 6;
        
        private final long sessionId;
        private final long subjectUid;
        private final boolean[] modifiedFlags;
        private final Flags newFlags;
        
        public FlagsUpdatedImpl(final long sessionId, final long subjectUid, 
                final Flags original, final Flags updated) {
            this(sessionId, subjectUid, updated, isChanged(original, updated, Flags.Flag.ANSWERED),
                    isChanged(original, updated, Flags.Flag.DELETED), 
                    isChanged(original, updated, Flags.Flag.DRAFT),
                    isChanged(original, updated, Flags.Flag.FLAGGED), 
                    isChanged(original, updated, Flags.Flag.RECENT),
                    isChanged(original, updated, Flags.Flag.SEEN));
        }
        
        public FlagsUpdatedImpl(final long sessionId, final long subjectUid, final Flags newFlags,
                boolean answeredUpdated, boolean deletedUpdated, boolean draftUpdated,
                boolean flaggedUpdated, boolean recentUpdated, boolean seenUpdated) {
            super();
            this.sessionId = sessionId;
            this.subjectUid = subjectUid;
            this.modifiedFlags = new boolean[NUMBER_OF_SYSTEM_FLAGS];
            this.modifiedFlags[0] = answeredUpdated;
            this.modifiedFlags[1] = deletedUpdated;
            this.modifiedFlags[2] = draftUpdated;
            this.modifiedFlags[3] = flaggedUpdated;
            this.modifiedFlags[4] = recentUpdated;
            this.modifiedFlags[5] = seenUpdated;
            this.newFlags = newFlags;
        }

        public long getSubjectUid() {
            return subjectUid;
        }

        public long getSessionId() {
            return sessionId;
        }

        public Iterator flagsIterator() {
            return new FlagsIterator();
        }
        
        public Flags getNewFlags() {
            return newFlags;
        }
        
        private class FlagsIterator implements Iterator {
            private int position;
            
            public FlagsIterator() {
                position = 0;
                nextPosition();
            }

            private void nextPosition() {
                if (position < NUMBER_OF_SYSTEM_FLAGS) {
                    if (!modifiedFlags[position]) {
                        position++;
                        nextPosition();
                    }
                }
            }
            
            public boolean hasNext() {
                return position < NUMBER_OF_SYSTEM_FLAGS;
            }

            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                final Flag result = FLAGS[position++];
                nextPosition();
                return result;
            }

            public void remove() {
                throw new UnsupportedOperationException("Read only");
            }
        }
    }

}
