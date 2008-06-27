package org.apache.james.mailboxmanager.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class UidToKeyBidiMapImpl  implements UidToKeyBidiMap {

        private Map keyToUid;

        private Map uidToKey;

        public UidToKeyBidiMapImpl() {
            keyToUid = new HashMap();
            uidToKey = new HashMap();
        }

        public synchronized String[] getKeys() {
            final ArrayList al = new ArrayList(keyToUid.keySet());
            final String[] keys = (String[]) al.toArray(new String[0]);
            return keys;
        }

        public synchronized void retainAllListedAndAddedByKeys(
                final String[] before, final Collection listed) {
            Collection added = new HashSet(keyToUid.keySet());
            added.removeAll(Arrays.asList(before));
            Collection retain = new HashSet(listed);
            retain.addAll(added);
            keyToUid.keySet().retainAll(retain);
            uidToKey.keySet().retainAll(keyToUid.values());
        }

        public synchronized void removeByKey(String key) {
            long uid = getByKey(key);
            if (uid > -1) {
                uidToKey.remove(new Long(uid));
            }
            keyToUid.remove(key);
        }

        public synchronized long getByKey(String key) {
            Long lo = (Long) keyToUid.get(key);
            long l = -1;
            if (lo != null) {
                l = lo.longValue();
            }
            return l;
        }

        public synchronized String getByUid(long uid) {

            return (String) uidToKey.get(new Long(uid));
        }

        public synchronized boolean containsKey(String key) {
            return keyToUid.containsKey(key);
        }

        public synchronized void put(String key, long uid) {
            keyToUid.put(key, new Long(uid));
            uidToKey.put(new Long(uid), key);
        }


}
