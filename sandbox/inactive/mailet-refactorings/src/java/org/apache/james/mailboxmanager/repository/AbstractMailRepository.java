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

package org.apache.james.mailboxmanager.repository;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import javax.mail.MessagingException;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.mailrepository.javamail.LockAdapter;
import org.apache.james.mailrepository.javamail.LockInterface;
import org.apache.mailet.Mail;
import org.apache.mailet.MailRepository;

/**
 * AbstractMailRepository that uses LockInterface
 * 
 */
public abstract class AbstractMailRepository implements MailRepository, LogEnabled
         {

    /**
     * Whether 'deep debugging' is turned on.
     */
    protected final static boolean DEEP_DEBUG = true;

    private static Random random;

    protected Log log;
    
    /**
     * A lock used to control access to repository elements, locking access
     * based on the key
     */
    private LockInterface lock;
    

    protected Log getLogger() {
        if (log==null) {
            log=new SimpleLog("AbstractMailRepository");
        }
        return log;
    }

    /**
     * gets the Lock and creates it, if not present. LockInterface offers functionality
     * of org.apache.james.util.Lock
     */
    protected LockInterface getLock() {
        if (lock==null) {
            lock = new LockAdapter();
        }
        return lock;
    }
    
    /**
     * possibility to replace Lock implementation. At the moment only used for testing 
     */
    void setLock(LockInterface lock) {
        this.lock=lock;
    }
    /**
     * sets log
     */
    public void enableLogging(Logger log) {
        this.log=new AvalonLogger(log);
        log.debug("MaiLRepository enableLogging");
    }

    /**
     * Removes a specified message
     * 
     * @param mail
     *            the message to be removed from the repository
     * @throws MessagingException
     */
    public void remove(Mail mail) throws MessagingException {
        getLogger().debug(this.getClass().getName()+" remove by Mail");
        remove(mail.getName());
    }
    
    
    /**
     * Remove a list of messages from disk The collection is simply a list of
     * mails to delete
     * 
     * @param mails
     * @throws MessagingException
     */
    public void remove(final Collection mails) throws MessagingException {
        if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
            StringBuffer logBuffer = new StringBuffer(128).append(
                    this.getClass().getName()).append(
                    " Removing entry for key ").append(mails);

            getLogger().debug(logBuffer.toString());
        }
        Iterator mailList = mails.iterator();

        /*
         * remove every email from the Collection
         */
        while (mailList.hasNext()) {
            remove(((Mail) mailList.next()).getName());
        }
    }
  
  
    /**
     * Releases a lock on a message identified by a key
     * 
     * @param key
     *            the key of the message to be unlocked
     * 
     * @return true if successfully released the lock, false otherwise
     */
    public boolean unlock(String key) {
        if (getLock().unlock(key)) {
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer debugBuffer = new StringBuffer(256).append(
                        "Unlocked ").append(key).append(" for ").append(
                        Thread.currentThread().getName()).append(" @ ").append(
                        new java.util.Date(System.currentTimeMillis()));
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
     * @param key
     *            the key of the message to be locked
     * 
     * @return true if successfully obtained the lock, false otherwise
     */
    public boolean lock(String key) {
        if (getLock().lock(key)) {
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer debugBuffer = new StringBuffer(256).append(
                        "Locked ").append(key).append(" for ").append(
                        Thread.currentThread().getName()).append(" @ ").append(
                        new java.util.Date(System.currentTimeMillis()));
                getLogger().debug(debugBuffer.toString());
            }
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * lazy-loads random 
     */
    protected static synchronized Random getRandom() {
        if (random == null) {
            random = new Random();
        }
        return random;

    }
    

}
