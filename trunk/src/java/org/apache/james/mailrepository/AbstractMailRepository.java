/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.mailrepository;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.MailRepository;
import org.apache.james.util.Lock;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 */
public abstract class AbstractMailRepository extends AbstractLogEnabled
        implements MailRepository, Serviceable, Configurable, Initializable {

    /**
     * Whether 'deep debugging' is turned on.
     */
    protected static final boolean DEEP_DEBUG = false;
    
    /**
     * A lock used to control access to repository elements, locking access
     * based on the key 
     */
    private Lock lock;

    protected Store store; // variable is not used beyond initialization
    
    void setStore(Store store) {
        this.store = store;
    }


    public void initialize() throws Exception {
        lock = new Lock();
    }


    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager )
     */
    public void service( final ServiceManager componentManager )
            throws ServiceException {
        setStore((Store)componentManager.lookup( Store.ROLE ));
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
            return true;
        } else {
            return false;
        }
    }


    /**
     * Store this message to the database.  Optionally stores the message
     * body to the filesystem and only writes the headers to the database.
     */
    public void store(Mail mc) throws MessagingException {
        boolean wasLocked = true;
        String key = mc.getName();
        try {
            synchronized(this) {
                  wasLocked = lock.isLocked(key);
                  if (!wasLocked) {
                      //If it wasn't locked, we want a lock during the store
                      lock(key);
                  }
            }
            internalStore(mc);
            if ((DEEP_DEBUG) && (getLogger().isDebugEnabled())) {
                StringBuffer logBuffer =
                    new StringBuffer(64)
                            .append("Mail ")
                            .append(key)
                            .append(" stored.");
                getLogger().debug(logBuffer.toString());
            }
        } catch (MessagingException e) {
            getLogger().error("Exception caught while storing mail "+key,e);
            throw e;
        } catch (Exception e) {
            getLogger().error("Exception caught while storing mail "+key,e);
            throw new MessagingException("Exception caught while storing mail "+key,e);
        } finally {
            if (!wasLocked) {
                // If it wasn't locked, we need to unlock now
                unlock(key);
                synchronized (this) {
                    notify();
                }
            }
        }
    }


    protected abstract void internalStore(Mail mc) throws MessagingException, IOException;


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
                internalRemove(key);
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


    protected abstract void internalRemove(String key) throws MessagingException;


}
