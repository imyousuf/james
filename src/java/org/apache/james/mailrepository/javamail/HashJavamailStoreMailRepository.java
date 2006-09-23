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

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailImpl;
import org.apache.james.mailrepository.javamail.HashJavamailStoreMailRepository.KeyToMsgMap.MsgObj;
import org.apache.mailet.Mail;

import com.sun.mail.util.CRLFOutputStream;

/**
 * MailRepository implementation to store mail in a Javamail store. <br>
 * should work with every JavamailStore implementation that has deterministic
 * message content. (checksum save). This implementation should be considered as
 * EXPERIMENTAL.
 * 
 * @author Joachim Draeger <jd at joachim-draeger.de>
 * 
 * TODO examine for thread-safety
 */
public class HashJavamailStoreMailRepository extends
        AbstractJavamailStoreMailRepository {

    /**
     * tridirectional map of messages key, hash and number saved in internaly
     * used class MsgObj
     */
    protected KeyToMsgMap keyToMsgMap = null;

    
    private boolean getMessageCountOnClosed =true;
    
    /**
     * get the count of messages 
     * 
     * @return the count of messages
     * 
     * @throws MessagingException
     */
    protected int getMessageCount() throws MessagingException {
        try {
            getFolderGateKeeper().use();
            int n=-1;
            if (getMessageCountOnClosed) {
                n=getFolderGateKeeper().getFolder().getMessageCount();
                if (n==-1) {
                    getMessageCountOnClosed=false;
                }
            }
            if (!getMessageCountOnClosed) {
                n=getFolderGateKeeper().getOpenFolder().getMessageCount();
            }
            return n;
        } finally {
            getFolderGateKeeper().free();
        }
    }
    
    /**
     * Stores a message by Javamails appendMessages method. Tries to guess
     * resulting messagenumber and saves result in keyToMsgMap. If Folder
     * supports getMessageCount on closed folder, this could be quite efficient
     * 
     * @see org.apache.james.services.MailRepository#store(Mail)
     */
    public synchronized void store(Mail mc) throws MessagingException {

        final String key = mc.getName();
        boolean wasLocked = true;
        log.debug("Store: (hash) " + key);
        if (!mc.getMessage().isSet(Flag.RECENT)) {
            log.debug("Message didn't have RECENT flag");
            mc.getMessage().setFlag(Flag.RECENT,true);
        }
        // because we use/free/use we need to know the state in finally
        boolean use=false;
        try {
            
            // Shouldn't we care when another Thread has locked this key and
            // stop here?
            synchronized (this) {
                wasLocked = getLock().isLocked(key);
                if (!wasLocked) {
                    // If it wasn't locked, we want a lock during the store
                    lock(key);
                }
            }
            
            
            // Yes, appendMessages works on a closed inbox. But we need
            // getMessageCount() and that is allowed
            // to be -1 on a closed Folder when counting messages is expensive

            int countBefore = getMessageCount();
            
            getFolderGateKeeper().use();
            use=true;
            // insert or update, don't call remove(key) because of locking
            if (getKeyToMsgMap().contains(key)) {
                log.debug("store is a update");
                Message mm = getMessageFromInbox(key);
                if (mm != null) {
                    countBefore--;
                    mm.setFlag(Flags.Flag.DELETED, true);
                    mc.getMessage().setFlag(Flags.Flag.RECENT, false);
                }
                getKeyToMsgMap().removeByKey(key, true);
            }
            getFolderGateKeeper().getFolder().appendMessages(new Message[] { mc.getMessage() });
            use=false;
            getFolderGateKeeper().free();

            // Try to guess resulting message number
            int no = -1;
            int count=getMessageCount();
            if (count - countBefore == 1) {
                no = count;
                log.debug("Assigned message number "+ count);
            } else {
                log.debug("count - countBefore = "+ (count - countBefore ));
            }

            getKeyToMsgMap().put(mc.getMessage(), mc.getName(), no);
        } catch (MessagingException e) {
            log.error("Exception in HashJavamailStore: ", e);
            throw e;
        } finally {
            if (!wasLocked) {
                // If it wasn't locked, we need to unlock now
                unlock(key);
                synchronized (this) {
                    notify();
                }
            }
            if (use) {
                getFolderGateKeeper().free();
            }
            log.debug("closed.");
        }
        log.debug("store finished");
    }

    /**
     * calls rehash and uses stored keys in KeyToMsgMap
     * 
     * @see org.apache.james.services.MailRepository#list()
     */
    public Iterator list() throws MessagingException {
        try {
            getFolderGateKeeper().use();
            log.debug("list()");
            rehash(null);
            final String[] keys = getKeyToMsgMap().getKeys();
            final Iterator it = Arrays.asList(keys).iterator();
            return it;
        } catch (MessagingException e) {
            throw e;
        } finally {
            getFolderGateKeeper().free();
        }

    }

    /**
     * uses getMessageFromInbox, returns null if not found
     * 
     * @see org.apache.james.services.MailRepository#retrieve(String)
     */
    public Mail retrieve(String key) throws MessagingException {
        log.debug("retrieve: " + key);
        Mail m = null;
        try {
            getFolderGateKeeper().use();
            MimeMessage mm = getMessageFromInbox(key);
            if (mm != null) {
                m = new MailImpl(mm);
                m.setName(key);
            } else {
                log.debug("could not retrieve a MimeMessage from folder");
            }

        } catch (MessagingException e) {
            throw e;
        } finally {
           getFolderGateKeeper().free();
        }
        return m;
    }

    /**
     * Removes a message identified by key. uses getMessageFromInbox and does a
     * setFlag(Flags.Flag.DELETED, true); on message. removes message from
     * KeyToMsgMap. Messagenumbers are recalculated for next guesses.
     * 
     * @see org.apache.james.services.MailRepository#remove(String)
     */
    public synchronized void  remove(String key) throws MessagingException {
        log.debug("HashJavamailStore remove key:" + key);
        if (lock(key)) {
            try {
                getFolderGateKeeper().use();
                Message mm = getMessageFromInbox(key);
                if (mm != null) {
                    // will be deleted on expunge
                    mm.setFlag(Flags.Flag.DELETED, true);
                }
                getKeyToMsgMap().removeByKey(key, true);
            } catch (MessagingException e) {
                throw e;
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
     * Calls getMessages(); on Folder and rehashes messages an renews message
     * numbers calls retainAllListedAndAddedByKeys on KeyToMsgMap to remove keys
     * not found in Folder
     * 
     * @param filterkey
     *            key of message that should be returned, can be null
     * @return a message if found by filterkey
     * @throws MessagingException
     */
    protected MimeMessage rehash(String filterkey) throws MessagingException {
        if (DEEP_DEBUG)
            log.debug("doing rehash");
        String[] keysBefore = getKeyToMsgMap().getKeys();
        MimeMessage mm = null;
        Message[] msgs = getFolderGateKeeper().getOpenFolder().getMessages();
        String[] keys = new String[msgs.length];
        for (int i = 0; i < msgs.length; i++) {
            Message message = msgs[i];
            MsgObj mo = getKeyToMsgMap()
                    .put((MimeMessage) message, null, i + 1);
            keys[i] = mo.key;
            if (DEEP_DEBUG)
                log.debug("No " + mo.no + " key:" + mo.key);
            if (mo.key.equals(filterkey)) {
                if (DEEP_DEBUG)
                    log.debug("Found message!");
                mm = (MimeMessage) message;
            }
        }
        getKeyToMsgMap().retainAllListedAndAddedByKeys(keysBefore, keys);
        return mm;
    }

    /**
     * Fetches a message from inbox. Fast fails if key is unknown in
     * KeyToMsgMap. Tries to get message at last known position, if that was not
     * successfull calls rehash
     * 
     * @param key
     *            message key
     * @return message if found, otherwise null
     * @throws MessagingException
     */
    protected MimeMessage getMessageFromInbox(String key)
            throws MessagingException {
        MsgObj mo = getKeyToMsgMap().getByKey(key);
        if (mo == null) {
            log.debug("Key not found");
            return null;
        }
        MimeMessage mm = null;
        if (cacheMessages && mo.message != null) {
            // not used at the moment
            mm = mo.message;
        } else {
            try {
                getFolderGateKeeper().use();
                Object hash = null;
                if (mo.no >= 0) {
                    try {
                        mm = (MimeMessage) getFolderGateKeeper().getOpenFolder()
                                .getMessage(mo.no);
                        hash = calcMessageHash(mm);
                        if (!hash.equals(mo.hash)) {
                            log
                                    .debug("Message at guessed position does not match "
                                            + mo.no);
                            mm = null;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        log.debug("no Message found at guessed position "
                                + mo.no);
                    }
                } else {
                    log.debug("cannot guess message number");
                }
                if (mm == null) {
                    mm = rehash(mo.key);
                    if (mm == null)
                        log.debug("rehashing was fruitless");
                }
            } finally {
                getFolderGateKeeper().free();
            }
        }
        return mm;
    }

    /**
     * lazy loads KeyToMsgMap
     * 
     * @return keyToMsgMap the KeyToMsgMap
     */
    protected KeyToMsgMap getKeyToMsgMap() {
        if (keyToMsgMap == null) {
            keyToMsgMap = new KeyToMsgMap();
        }
        return keyToMsgMap;
    }

    private static final class HasherOutputStream extends FilterOutputStream {
        int hashCode = 0;

        public HasherOutputStream(OutputStream out) {
            super(out);
        }

        // TODO verify this method (off/len usage)
        public void write(byte[] b, int off, int len) throws IOException {
            for (int i = off; i < b.length && i < len+off; i++) {
                hashCode += 77;
                hashCode ^= b[i];
            }
            super.write(b, off, len);
        }

        public void write(byte[] b) throws IOException {
            for (int i = 0; i < b.length; i++) {
                hashCode += 77;
                hashCode ^= b[i];
            }
            super.write(b);
        }

        public void write(int b) throws IOException {
            hashCode += 77;
            hashCode ^= b;
            super.write(b);
        }

        public int getHash() {
            return hashCode;
        }
    }

    protected class KeyToMsgMap {
        protected SortedMap noToMsgObj;

        protected Map keyToMsgObj;

        protected Map hashToMsgObj;

        protected KeyToMsgMap() {
            noToMsgObj = new TreeMap();
            keyToMsgObj = new HashMap();
            hashToMsgObj = new HashMap();
        }

        /**
         * Return true if a MsgObj is stored with the given key
         * 
         * @param key the key 
         * @return true if a MsgObj is stored with the given key
         */
        public synchronized boolean contains(String key) {
            return keyToMsgObj.containsKey(key);
        }

        /**
         * Cleans up database after rehash. Only keys listed by rehash or added
         * in the meantime are retained
         * 
         * @param keysBefore
         *            keys that have exist before rehash was called
         * @param listed
         *            keys that have listed by rehash
         */
        public synchronized void retainAllListedAndAddedByKeys(
                String[] keysBefore, String[] listed) {
            if (DEEP_DEBUG)
                log.debug("stat before retain: " + getStat());
            Set added = new HashSet(keyToMsgObj.keySet());
            added.removeAll(Arrays.asList(keysBefore));

            Set retain = new HashSet(Arrays.asList(listed));
            retain.addAll(added);

            Collection remove = new HashSet(keyToMsgObj.keySet());
            remove.removeAll(retain);
            // huh, are we turning in circles? :-)

            for (Iterator iter = remove.iterator(); iter.hasNext();) {
                removeByKey((String) iter.next(), false);
            }
            if (DEEP_DEBUG)
                log.debug("stat after retain: " + getStat());
        }

        /**
         * only used for debugging
         * 
         * @return a String representing the sizes of the internal maps
         */
        public String getStat() {
            String s = "keyToMsgObj:" + keyToMsgObj.size() + " hashToMsgObj:"
                    + hashToMsgObj.size() + " noToMsgObj:" + noToMsgObj.size();
            return s;
        }

        /**
         * removes a message from the maps.
         * 
         * @param key
         *            key of message
         * @param decrease
         *            if true, all message number greater than this are
         *            decremented
         */
        public synchronized void removeByKey(String key, boolean decrement) {
            MsgObj mo = getByKey(key);
            keyToMsgObj.remove(mo.key);
            noToMsgObj.remove(new Integer(mo.no));
            hashToMsgObj.remove(mo.hash);
            if (decrement) {
                // tailMap finds all entries that have message number greater
                // than removed one and decrements them
                MsgObj[] dmos = (MsgObj[]) noToMsgObj.tailMap(
                        new Integer(mo.no)).values().toArray(new MsgObj[0]);
                for (int i = 0; i < dmos.length; i++) {
                    MsgObj dmo = dmos[i];
                    noToMsgObj.remove(new Integer(dmo.no));
                    dmo.no--;
                    noToMsgObj.put(new Integer(dmo.no), dmo);
                }
            }
        }

        /**
         * Return an String[] of all stored keys
         * 
         * @return keys a String[] of all stored keys
         */
        public synchronized String[] getKeys() {
            return (String[]) keyToMsgObj.keySet().toArray(new String[0]);
        }

        /**
         * Return the MsgObj associted with the given key
         * 
         * @param key the key 
         * @return msgObj the MsgObj for the given key
         */
        public synchronized MsgObj getByKey(String key) {
            return (MsgObj) keyToMsgObj.get(key);
        }

        /**
         * At first it tries to lookup message by key otherwise by hash or
         * stores it as new
         * 
         * @param mm
         *            message
         * @param key
         *            if null it will be generated when not found by hash
         * @param no
         *            current number of this message
         * @return fetched/created MsgObj
         * @throws MessagingException
         */
        public synchronized MsgObj put(final MimeMessage mm, String key,
                final int no) throws MessagingException {
            final Object hash = calcMessageHash(mm);
            MsgObj mo;
            if (key != null) {
                mo = getMsgObj(key);
            } else {
                mo = (MsgObj) hashToMsgObj.get(hash);
                if (mo == null) {
                    key = generateKey(hash.toString());
                }
            }
            if (mo == null) {
                mo = new MsgObj();
                keyToMsgObj.put(key, mo);
                mo.key = key;
            }
            if (!hash.equals(mo.hash)) {
                if (mo.hash != null) {
                    hashToMsgObj.remove(mo.hash);
                }
                mo.hash = hash;
                hashToMsgObj.put(hash, mo);
            }
            if (no != mo.no) {
                if (mo.no > -1) {
                    noToMsgObj.remove(new Integer(mo.no));
                }
                mo.no = no;
                noToMsgObj.put(new Integer(no), mo);
            }
            if (cacheMessages) {
                mo.message = mm;
            }
            return mo;

        }

        /**
         * TODO: Check why we have to methods which do the same
         * 
         * @see #getByKey(String)
         */
        public synchronized MsgObj getMsgObj(String key) {
            return (MsgObj) keyToMsgObj.get(key);
        }

        /**
         * used to internal represent a message
         * 
         */
        protected class MsgObj {
            MimeMessage message;

            int no = -1;

            Object hash;

            String key;
        }

    }

    /**
     * currently uses Arrays.hashCode to build an Integer. Resulting Method
     * should provide a good hashCode and a correct equals method
     * 
     * @param mm
     *            message to hash
     * @return an Object provides and correct equals method.
     * @throws MessagingException
     */
    protected static Object calcMessageHash(MimeMessage mm)
            throws MessagingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // This nested class has been created because Java 2 v1.4 has 
        // no Arrays.hashCode() method.
        // We can remove this as soon as we'll require Java 5.
        HasherOutputStream hos = new HasherOutputStream(new CRLFOutputStream(baos));
        try {
            mm.writeTo(hos);
        } catch (IOException e) {
            throw new MessagingException("error while calculating hash ", e);
        }
        
        Integer i = new Integer(hos.getHash());
        return i;
    }

    /**
     * builds a key for unknow messages
     * 
     * @param hash Hash to be included in key
     * @return contains "james-hashed:", the hash, the time and a random long
     */
    protected static String generateKey(String hash) {
        String key = "james-hashed:" + hash + ";" + System.currentTimeMillis()
                + ";" + getRandom().nextLong();
        return key;
    }

    /**
     * just uses a FolderAdapter to wrap around folder
     * 
     * @see org.apache.james.mailrepository.javamail.AbstractJavamailStoreMailRepository#createAdapter(Folder)
     */
    protected FolderInterface createAdapter(Folder folder)
            throws NoSuchMethodException {
        return new FolderAdapter(folder);
    }

}
