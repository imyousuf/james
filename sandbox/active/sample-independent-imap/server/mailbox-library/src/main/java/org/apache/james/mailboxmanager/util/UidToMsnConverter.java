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

package org.apache.james.mailboxmanager.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.james.mailboxmanager.MailboxListener;

public class UidToMsnConverter implements MailboxListener {
    protected SortedMap msnToUid;

    protected SortedMap uidToMsn;

    protected long highestUid = 0;
    
    protected int highestMsn = 0;

    public UidToMsnConverter(final Collection uids) {
        msnToUid = new TreeMap();
        uidToMsn = new TreeMap();
        if (uids != null) {
            int msn = 1;
            List uidsInOrder = new ArrayList(uids);
            Collections.sort(uidsInOrder);
            for (final Iterator it=uidsInOrder.iterator();it.hasNext();msn++) {
                final Long uid = (Long) it.next();
                highestUid = uid.longValue();
                highestMsn = msn;
                final Integer msnInteger = new Integer(msn);
                msnToUid.put(msnInteger, uid);
                uidToMsn.put(uid, msnInteger);
            }
        }
    }

    public synchronized long getUid(int msn) {
        if (msn == -1) {
            return -1;
        }
        Long uid = (Long) msnToUid.get(new Integer(msn));
        if (uid != null) {
            return uid.longValue();
        } else {
            if (msn > 0) {
                return highestUid;
            } else {
                return 0;
            }
        }
    }

    public synchronized int getMsn(long uid) {
        Integer msn = (Integer) uidToMsn.get(new Long(uid));
        if (msn != null) {
            return msn.intValue();
        } else {
            return -1;
        }

    }

    protected synchronized void add(int msn, long uid) {
        if (uid > highestUid) {
            highestUid = uid;
        }
        final Integer msnInteger = new Integer(msn);
        final Long uidLong = new Long(uid);
        msnToUid.put(msnInteger, uidLong);
        uidToMsn.put(uidLong, msnInteger);
    }

    
    
    public synchronized void expunge(long uid) {
        int msn=getMsn(uid);
        remove(msn,uid);
        List renumberMsns=new ArrayList(msnToUid.tailMap(new Integer(msn+1)).keySet());
        for (Iterator iter = renumberMsns.iterator(); iter.hasNext();) {
            int aMsn = ((Integer) iter.next()).intValue();
            long aUid= getUid(aMsn);
            remove(aMsn,aUid);
            add(aMsn-1,aUid);
        }
        highestMsn--;
        assertValidity();
    }
    
    protected void remove(int msn,long uid) {
        uidToMsn.remove(new Long(uid));
        msnToUid.remove(new Integer(msn));
    }
    
    synchronized void assertValidity() {
        Integer[] msns=(Integer[])msnToUid.keySet().toArray(new Integer[0]);
        for (int i = 0; i < msns.length; i++) {
            if (msns[i].intValue()!=(i+1)) {
                throw new AssertionError("Msn at position "+(i+1)+" was "+msns[i].intValue());
            }
        }
        if (msns.length > 0) {
            if (msns[msns.length - 1].intValue() != highestMsn) {
                throw new AssertionError("highestMsn " + highestMsn
                        + " msns[msns.length-1] " + msns[msns.length - 1]);
            }
        } else {
            if (highestMsn != 0) {
                throw new AssertionError(
                        "highestMsn in empty map has to be 0 but is"
                                + highestMsn);
            }
        }
        if (!msnToUid.keySet().equals(new TreeSet(uidToMsn.values()))) {
            System.out.println(msnToUid.keySet());
            System.out.println(uidToMsn.values());
            throw new AssertionError("msnToUid.keySet() does not equal uidToMsn.values()");
        }
        if (!uidToMsn.keySet().equals(new TreeSet(msnToUid.values()))) {
            System.out.println(uidToMsn.keySet());
            System.out.println(msnToUid.values());
            throw new AssertionError("uidToMsn.keySet() does not equal msnToUid.values()");
        }

    }

    public synchronized void add(long uid) {
        if (!uidToMsn.containsKey(new Long(uid))) {
            highestMsn++;
            add(highestMsn, uid);
        }
    }

    int size() {
        return uidToMsn.size();
    }

    /**
     * @see org.apache.james.mailboxmanager.MailboxListener#event(org.apache.james.mailboxmanager.MailboxListener.Event)
     */
    public void event(Event event) {
        if (event instanceof MessageEvent) {
            final MessageEvent messageEvent = (MessageEvent) event;
            final long uid = messageEvent.getSubjectUid();
            if (event instanceof Added) {
                add(uid);
            }
        }
    }
}
