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

package org.apache.james.test.mock.james;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.core.MailImpl;
import org.apache.james.services.SpoolRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.util.Lock;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Implementation of a MailRepository on a FileSystem.
 *
 * Requires a configuration element in the .conf.xml file of the form:
 *  &lt;repository destinationURL="file://path-to-root-dir-for-repository"
 *              type="MAIL"
 *              model="SYNCHRONOUS"/&gt;
 * Requires a logger called MailRepository.
 *
 * @version 1.0.0, 24/04/1999
 */
public class InMemorySpoolRepository
    implements SpoolRepository, Disposable {

    /**
     * Whether 'deep debugging' is turned on.
     */
    protected final static boolean DEEP_DEBUG = true;
    private Lock lock;
    private MockLogger logger;
    private Hashtable spool;

    private MockLogger getLogger() {
        if (logger == null) {
            logger = new MockLogger();
        }
        return logger;
    }

    /**
     * Releases a lock on a message identified by a key
     *
     * @param key the key of the message to be unlocked
     *
     * @return true if successfully released the lock, false otherwise
     */
    public boolean unlock(String key) {
        if (lock.unlock(key)) {
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer debugBuffer =
                    new StringBuffer(256)
                            .append("Unlocked ")
                            .append(key)
                            .append(" for ")
                            .append(Thread.currentThread().getName())
                            .append(" @ ")
                            .append(new java.util.Date(System.currentTimeMillis()));
                getLogger().debug(debugBuffer.toString());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Obtains a lock on a message identified by a key
     *
     * @param key the key of the message to be locked
     *
     * @return true if successfully obtained the lock, false otherwise
     */
    public boolean lock(String key) {
        if (lock.lock(key)) {
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer debugBuffer =
                    new StringBuffer(256)
                            .append("Locked ")
                            .append(key)
                            .append(" for ")
                            .append(Thread.currentThread().getName())
                            .append(" @ ")
                            .append(new java.util.Date(System.currentTimeMillis()));
                getLogger().debug(debugBuffer.toString());
            }
//            synchronized (this) {
//                notifyAll();
//            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Stores a message in this repository. Shouldn't this return the key
     * under which it is stored?
     *
     * @param mc the mail message to store
     */
    public void store(Mail mc) throws MessagingException {
        try {
            String key = mc.getName();
            //Remember whether this key was locked
            boolean wasLocked = true;
            synchronized (this) {
                wasLocked = lock.isLocked(key);
    
                if (!wasLocked) {
                    //If it wasn't locked, we want a lock during the store
                    lock(key);
                }
            }
            try {
                // Remove any previous copy of this mail
                if (spool.containsKey(key)) {
                    // do not use this.remove because this would
                    // also remove a current lock.
                    Object o = spool.remove(key);
                    ContainerUtil.dispose(o);
                }
                // Clone the mail (so the caller could modify it).
                MailImpl m = new MailImpl(mc,mc.getName());
                m.setState(mc.getState());
                m.setLastUpdated(mc.getLastUpdated());
                m.setErrorMessage(mc.getErrorMessage());
                spool.put(mc.getName(),m);
            } finally {
                if (!wasLocked) {
                    // If it wasn't locked, we need to unlock now
                    unlock(key);
                    synchronized (this) {
                        notify();
                    }
                }
            }

            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer logBuffer =
                    new StringBuffer(64)
                            .append("Mail ")
                            .append(key)
                            .append(" stored.");
                getLogger().debug(logBuffer.toString());
            }

        } catch (Exception e) {
            getLogger().error("Exception storing mail: " + e,e);
            throw new MessagingException("Exception caught while storing Message Container: ",e);
        }
    }

    /**
     * Retrieves a message given a key. At the moment, keys can be obtained
     * from list() in superinterface Store.Repository
     *
     * @param key the key of the message to retrieve
     * @return the mail corresponding to this key, null if none exists
     */
    public Mail retrieve(String key) throws MessagingException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Retrieving mail: " + key);
        }
        try {
            Mail mc = null;
            try {
                mc = new MailImpl((Mail) spool.get(key),key);
                mc.setState(((Mail) spool.get(key)).getState());
                mc.setErrorMessage(((Mail) spool.get(key)).getErrorMessage());
                mc.setLastUpdated(((Mail) spool.get(key)).getLastUpdated());
            } 
            catch (RuntimeException re){
                StringBuffer exceptionBuffer = new StringBuffer(128);
                if(re.getCause() instanceof Error){
                    exceptionBuffer.append("Error when retrieving mail, not deleting: ")
                            .append(re.toString());
                }else{
                    exceptionBuffer.append("Exception retrieving mail: ")
                            .append(re.toString())
                            .append(", so we're deleting it.");
                    remove(key);
                }
                getLogger().warn(exceptionBuffer.toString());
                return null;
            }
            return mc;
        } catch (Exception me) {
            getLogger().error("Exception retrieving mail: " + me);
            throw new MessagingException("Exception while retrieving mail: " + me.getMessage());
        }
    }

    /**
     * Removes a specified message
     *
     * @param mail the message to be removed from the repository
     */
    public void remove(Mail mail) throws MessagingException {
        remove(mail.getName());
    }


    /**
     * Removes a Collection of mails from the repository
     * @param mails The Collection of <code>MailImpl</code>'s to delete
     * @throws MessagingException
     * @since 2.2.0
     */
    public void remove(Collection mails) throws MessagingException {
        Iterator delList = mails.iterator();
        while (delList.hasNext()) {
            remove((Mail)delList.next());
        }
    }

    /**
     * Removes a message identified by key.
     *
     * @param key the key of the message to be removed from the repository
     */
    public void remove(String key) throws MessagingException {
        if (lock(key)) {
            try {
                if (spool != null) {
                    Object o = spool.remove(key);
                    ContainerUtil.dispose(o);
                }
            } finally {
                unlock(key);
            }
        } else {
            StringBuffer exceptionBuffer =
                new StringBuffer(64)
                        .append("Cannot lock ")
                        .append(key)
                        .append(" to remove it");
            throw new MessagingException(exceptionBuffer.toString());
        }
    }

    /**
     * List string keys of messages in repository.
     *
     * @return an <code>Iterator</code> over the list of keys in the repository
     *
     */
    public Iterator list() {
        // Fix ConcurrentModificationException by cloning 
        // the keyset before getting an iterator
        final ArrayList clone;
        synchronized(spool) {
            clone = new ArrayList(spool.keySet());
        }
        return clone.iterator();
    }

    
    /**
     * <p>Returns an arbitrarily selected mail deposited in this Repository.
     * Usage: SpoolManager calls accept() to see if there are any unprocessed 
     * mails in the spool repository.</p>
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return the mail
     */
    public synchronized Mail accept() throws InterruptedException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept() called");
        }
        return accept(new SpoolRepository.AcceptFilter () {
            public boolean accept (String _, String __, long ___, String ____) {
                return true;
            }

            public long getWaitTime () {
                return 0;
            }
        });
    }

    /**
     * <p>Returns an arbitrarily selected mail deposited in this Repository that
     * is either ready immediately for delivery, or is younger than it's last_updated plus
     * the number of failed attempts times the delay time.
     * Usage: RemoteDeliverySpool calls accept() with some delay and should block until an
     * unprocessed mail is available.</p>
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return the mail
     */
    public synchronized Mail accept(final long delay) throws InterruptedException
    {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept(delay) called");
        }
        return accept(new SpoolRepository.AcceptFilter () {
            long youngest = 0;
                
                public boolean accept (String key, String state, long lastUpdated, String errorMessage) {
                    if (state.equals(Mail.ERROR)) {
                        //Test the time...
                        long timeToProcess = delay + lastUpdated;
                
                        if (System.currentTimeMillis() > timeToProcess) {
                            //We're ready to process this again
                            return true;
                        } else {
                            //We're not ready to process this.
                            if (youngest == 0 || youngest > timeToProcess) {
                                //Mark this as the next most likely possible mail to process
                                youngest = timeToProcess;
                            }
                            return false;
                        }
                    } else {
                        //This mail is good to go... return the key
                        return true;
                    }
                }
        
                public long getWaitTime () {
                    if (youngest == 0) {
                        return 0;
                    } else {
                        long duration = youngest - System.currentTimeMillis();
                        youngest = 0; //get ready for next round
                        return duration <= 0 ? 1 : duration;
                    }
                }
            });
    }


    /**
     * Returns an arbitrarily select mail deposited in this Repository for
     * which the supplied filter's accept method returns true.
     * Usage: RemoteDeliverySpool calls accept(filter) with some a filter which determines
     * based on number of retries if the mail is ready for processing.
     * If no message is ready the method will block until one is, the amount of time to block is
     * determined by calling the filters getWaitTime method.
     *
     * <p>Synchronized to ensure thread safe access to the underlying spool.</p>
     *
     * @return  the mail
     */
    public synchronized Mail accept(SpoolRepository.AcceptFilter filter) throws InterruptedException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            getLogger().debug("Method accept(Filter) called");
        }
        while (!Thread.currentThread().isInterrupted()) try {
            for (Iterator it = list(); it.hasNext(); ) {
                String s = it.next().toString();
                if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                    StringBuffer logBuffer =
                        new StringBuffer(64)
                                .append("Found item ")
                                .append(s)
                                .append(" in spool.");
                    getLogger().debug(logBuffer.toString());
                }
                if (lock(s)) {
                    if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                        getLogger().debug("accept(Filter) has locked: " + s);
                    }
                    try {
                        Mail mail = retrieve(s);
                        // Retrieve can return null if the mail is no longer on the spool
                        // (i.e. another thread has gotten to it first).
                        // In this case we simply continue to the next key
                        if (mail == null || !filter.accept (mail.getName(),
                                                            mail.getState(),
                                                            mail.getLastUpdated().getTime(),
                                                            mail.getErrorMessage())) {
                            unlock(s);
                            continue;
                        }
                        return mail;
                    } catch (javax.mail.MessagingException e) {
                        unlock(s);
                        getLogger().error("Exception during retrieve -- skipping item " + s, e);
                    }
                }
            }

            //We did not find any... let's wait for a certain amount of time
            wait (filter.getWaitTime());
        } catch (InterruptedException ex) {
            throw ex;
        } catch (ConcurrentModificationException cme) {
            // Should never get here now that list methods clones keyset for iterator
            getLogger().error("CME in spooler - please report to http://james.apache.org", cme);
        }
        throw new InterruptedException();
    }

    /**
     * 
     */
    public InMemorySpoolRepository() {
        spool = new Hashtable();
        lock = new Lock();
    }

    public int size() {
        return spool.size();
    }

    public void clear() {
        if (spool != null) {
            Iterator i = list();
            while (i.hasNext()) {
                String key = (String) i.next();
                try {
                    remove(key);
                } catch (MessagingException e) {
                }
            }
        }
    }

    public void dispose() {
        clear();
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(super.toString());
        Iterator i = list();
        while (i.hasNext()) {
            result.append("\n\t"+i.next());
        }
        return result.toString();
    }
    
}
