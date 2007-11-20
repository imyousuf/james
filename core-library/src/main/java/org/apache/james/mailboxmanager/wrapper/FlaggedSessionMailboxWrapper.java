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

import java.util.Iterator;

import javax.mail.Flags;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.MessageResultImpl;
import org.apache.james.mailboxmanager.mailbox.FlaggedMailbox;

public class FlaggedSessionMailboxWrapper extends SessionMailboxWrapper {
    
    public FlaggedSessionMailboxWrapper(FlaggedMailbox flaggedMailbox) throws MailboxManagerException {
        super(flaggedMailbox);
    }
    
    
    public synchronized MessageResult[] expunge(GeneralMessageSet set, int result) throws MailboxManagerException {
        return addMsnToResults(((FlaggedMailbox) mailbox).expunge(toUidSet(set), noMsnResult(result)),result);
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
        return new MsnIterator(results, getNumberCache());
    }
    
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
            final MessageResultImpl result = new MessageResultImpl(next);
            result.setMsn(map.getMsn(result.getUid()));
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
}
