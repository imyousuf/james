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

package org.apache.james.mailrepository.javamail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;

/**
 * MailRepository implementation to store mail in a Javamail store which
 * provides the UID Plus method public long[] addUIDMessages.<br>
 * <br>
 * This implementation should be considered as EXPERIMENTAL.
 * 
 * TODO examine for thread-safety
 */
public class UIDPlusFolderMailRepository extends
        AbstractJavamailStoreMailRepository {

    /**
     * used to map keys to uid and vice versa
     */
    private UidToKeyBidiMap uidToKeyBidiMap = null;

    public static final int DELIVERY_MODE_CLOSED = 0;
    public static final int DELIVERY_MODE_TRY = 1;
    public static final int DELIVERY_MODE_OPEN = 2;
    
    private int deliveryMode=DELIVERY_MODE_TRY;
    
    protected long addUIDMessage(Message message) throws MessagingException {
        try {
            getFolderGateKeeper().use();
            long[] uids = null;
            if (deliveryMode != DELIVERY_MODE_OPEN) {
                try {
                    log.debug("Doing a addUIDMessages on maybe closed Folder: isopen="+getFolderGateKeeper().getFolder().isOpen());
                    uids = ((UIDPlusFolder) getFolderGateKeeper().getFolder())
                            .addUIDMessages(new Message[] { message });
                } catch (IllegalStateException e) {
                    if (deliveryMode == DELIVERY_MODE_CLOSED) {
                        log.error("deliveryMode=DELIVERY_MODE_CLOSED",e);
                        throw e;
                    } else {
                        log.debug("switching to DELIVERY_MODE_OPEN",e);
                        deliveryMode = DELIVERY_MODE_OPEN;
                    }

                }
            }
            if (deliveryMode == DELIVERY_MODE_OPEN) {
                log.debug("Doing a addUIDMessages on a open Folder");
                uids = ((UIDPlusFolder) getFolderGateKeeper().getOpenFolder())
                        .addUIDMessages(new Message[] { message });
            }
            if (uids == null || uids.length != 1) {
                throw new RuntimeException(
                        "Fatal error while storing Message Container: Message was not Appendet");
            }
            return uids[0];
        } finally {
            getFolderGateKeeper().free();
        }

    }
    


    /**
     * Stores a message in this repository.
     * 
     * @param mc
     *            the mail message to store
     * @throws MessagingException
     */
    public void store(Mail mc) throws MessagingException {
        
        log.debug("UIDPlusFolder store key:" + mc.getName());
        if (!mc.getMessage().isSet(Flag.RECENT)) {
            log.debug("Message didn't have RECENT flag");
            mc.getMessage().setFlag(Flag.RECENT,true);
        }
        String key = mc.getName();
        boolean wasLocked = true;
        try {
            getFolderGateKeeper().use();
            MimeMessage message = mc.getMessage();
            synchronized (this) {
                wasLocked = getLock().isLocked(key);
                if (!wasLocked) {
                    // If it wasn't locked, we want a lock during the store
                    lock(key);
                }
            }

            // insert or update, don't call remove(key) because of locking
            if (getUidToKeyBidiMap().containsKey(key)) {
                // message gets updated an folder stays open. 
                Message mm = getMessageFromInbox(key,
                        (UIDFolder) getFolderGateKeeper().getOpenFolder());
                if (mm != null) {
                    mm.setFlag(Flags.Flag.DELETED, true);
                    message.setFlag(Flags.Flag.RECENT, false);
                }
            }
            long uid = addUIDMessage(message);
            getUidToKeyBidiMap().put(key, uid);

            log.info("message stored: UID: " + uid + " Key:" + mc.getName());
        } finally {
            getFolderGateKeeper().free();
            if (!wasLocked) {
                // If it wasn't locked, we need to unlock now
                unlock(key);
                synchronized (this) {
                    notify();
                }
            }

        }
    }

    /**
     * lazy loads UidToKeyBidiMap
     */
    protected UidToKeyBidiMap getUidToKeyBidiMap() {
        if (uidToKeyBidiMap == null) {
            uidToKeyBidiMap = new UidToKeyBidiMapImpl();
        }
        return uidToKeyBidiMap;
    }

    /**
     * Used for testing
     * 
     * @param uidToKeyBidiMap
     */
    void setUidToKeyBidiMap(UidToKeyBidiMap uidToKeyBidiMap) {
        this.uidToKeyBidiMap = uidToKeyBidiMap;
    }

    /**
     * Retrieves a message given a key. At the moment, keys can be obtained from
     * list() in superinterface Store.Repository
     * 
     * @param key
     *            the key of the message to retrieve
     * @return the mail corresponding to this key, null if none exists
     * @throws MessagingException 
     */

    public Mail retrieve(String key) throws MessagingException {
        log.debug("UIDPlusFolder retrieve " + key);
        try {
            getFolderGateKeeper().use();
            MimeMessage mm = getMessageFromInbox(key,
                    (UIDFolder) getFolderGateKeeper().getOpenFolder());
            if (mm == null)
                return null;
            Mail mail = new MailImpl();
            mail.setMessage(mm);
            mail.setName(key);
            return mail;
        } finally {
            getFolderGateKeeper().free();
        }
    }

    /**
     * Removes a message identified by key.
     * 
     * @param key
     *            the key of the message to be removed from the repository
     */
    public void remove(String key) throws MessagingException {

        log.debug("UIDFolder remove key:" + key);// , new Throwable());
        if (lock(key)) {
            getFolderGateKeeper().use();
            try {
                Message mm = getMessageFromInbox(key,
                        (UIDFolder) getFolderGateKeeper().getOpenFolder());
                if (mm != null) {
                    mm.setFlag(Flags.Flag.DELETED, true);
                }
                getUidToKeyBidiMap().removeByKey(key);
            } finally {
                unlock(key);
                getFolderGateKeeper().free();
            }
        } else {
            log.debug("could not optain lock");
            throw new MessagingException("could not optain lock for remove");
        }
    }

    /**
     * List string keys of messages in repository.
     * 
     * @return an <code>Iterator</code> over the list of keys in the
     *         repository
     * @throws MessagingException 
     * 
     */
    public Iterator<String> list() throws MessagingException {
        log.debug("UIDPlusFolder list");
        try {
            getFolderGateKeeper().use();
            // needed for retainAllListedAndAddedByKeys(String[], Collection)
            String[] keysBefore = getUidToKeyBidiMap().getKeys();
            // get the messages
            Message[] msgs = getFolderGateKeeper().getOpenFolder().getMessages();
            Collection<String> keys = new ArrayList<String>(msgs.length);
            if (msgs == null)
                throw new RuntimeException("inbox.getMessages returned null");
            for (int i = 0; i < msgs.length; i++) {
                try {
                    long uidvalidity = ((UIDFolder) getFolderGateKeeper().getOpenFolder()).getUIDValidity();
                    // lookup uid
                    long uid = ((UIDFolder) getFolderGateKeeper().getOpenFolder()).getUID(msgs[i]);
                    String key = getUidToKeyBidiMap().getByUid(uid);
                    if (key == null) {
                        // generate new key
                        key = "james-uid:" + uidvalidity + ";" + uid + ";"
                                + System.currentTimeMillis() + ";"
                                + getRandom().nextLong();
                        getUidToKeyBidiMap().put(key, uid);
                    }
                    keys.add(key);
                    log.info("list: UID: " + uid + " Key:" + key);
                } catch (NoSuchElementException e) {
                    // no problem, messages could have been deleted in the
                    // meantime
                }
            }
            // retain only listed keys, and keys added in the meantime (it would
            // be fatal to loose those)
            // I don't care about meanwhile removed, those will fail on next
            // access
            // it's a good idea to keep count of cached small
            getUidToKeyBidiMap()
                    .retainAllListedAndAddedByKeys(keysBefore, keys);
            return keys.iterator();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } finally {
            getFolderGateKeeper().free();
        }
    }

    private MimeMessage getMessageFromInbox(String key, UIDFolder inbox)
            throws MessagingException {

        long uid = getUidToKeyBidiMap().getByKey(key);
        if (uid < 0) {
            return null;
        }

        MimeMessage mm = (MimeMessage) inbox.getMessageByUID(uid);
        log.info("getMessageFromInbox: UID: " + uid + " Key:" + key);
        if (mm == null) {
            getUidToKeyBidiMap().removeByKey(key);
            log.info("Message not Found");
        }
        return mm;
    }

    /**
     * 
     * maybe it could be replaced by BidiMap from commons-collections 3.0+
     */
    private class UidToKeyBidiMapImpl implements UidToKeyBidiMap {

        private Map<String,Long> keyToUid;

        private Map<Long,String> uidToKey;

        public UidToKeyBidiMapImpl() {
            keyToUid = new HashMap<String,Long>();
            uidToKey = new HashMap<Long,String>();
        }

        public synchronized String[] getKeys() {
            final ArrayList<String> al = new ArrayList<String>(keyToUid.keySet());
            final String[] keys = (String[]) al.toArray(new String[0]);
            return keys;
        }

        public synchronized void retainAllListedAndAddedByKeys(
                final String[] before, final Collection<String> listed) {
            Collection<String> added = new HashSet<String>(keyToUid.keySet());
            added.removeAll(Arrays.asList(before));
            Collection<String> retain = new HashSet<String>(listed);
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

    /**
     * returns a UIDPlusFolderAdapter wrapper
     * 
     * @see UIDPlusFolderAdapter
     */
    public FolderInterface createAdapter(Folder folder) {
        return new UIDPlusFolderAdapter(folder);
    }


}
