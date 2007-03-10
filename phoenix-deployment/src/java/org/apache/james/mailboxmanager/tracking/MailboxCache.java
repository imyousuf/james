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

package org.apache.james.mailboxmanager.tracking;

import java.util.HashMap;
import java.util.Iterator;
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
    
    public synchronized Map getOpenMailboxSessionCountMap() {
        Map countMap = new HashMap();
        for (Iterator iter = trackers.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            String name = (String) entry.getKey();
            MailboxTracker tracker = (MailboxTracker) entry.getValue();
            countMap.put(name,new Integer(tracker.getSessionCount()));
        }
        return countMap;
    }

}
