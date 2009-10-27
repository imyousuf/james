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



package org.apache.james.mailrepository;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.services.MailRepository;
import org.apache.james.util.Lock;
import org.apache.mailet.Mail;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.MessagingException;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class represent an AbstractMailRepository. All MailRepositories should extend this class. 
 */
public abstract class AbstractMailRepository implements MailRepository {

    /**
     * Whether 'deep debugging' is turned on.
     */
    protected static final boolean DEEP_DEBUG = false;
    
    /**
     * A lock used to control access to repository elements, locking access
     * based on the key 
     */
    private final Lock lock = new Lock();;

    protected Store store; // variable is not used beyond initialization
    
    private Log logger;

    private HierarchicalConfiguration configuration;
    
    @Resource(name="org.apache.commons.logging.Log")
    public void setLogger(Log logger) {
        this.logger = logger;
    }
    
    protected Log getLogger() {
        return logger;
    }
      
    @Resource(name="org.apache.commons.configuration.Configuration")
    public void setConfiguration(HierarchicalConfiguration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * Set the Store to use
     * 
     * @param store the Store
     */
    @Resource(name="org.apache.avalon.cornerstone.services.store.Store")
    public void setStore(Store store) {
        this.store = store;
    }
    
    protected void doConfigure(HierarchicalConfiguration config) throws ConfigurationException {
        
    }

    /**
     * @see org.apache.james.services.MailRepository#unlock(String)
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
     * @see org.apache.james.services.MailRepository#lock(String)
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
     * @see org.apache.james.services.MailRepository#store(Mail)
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

    @PostConstruct
    public void init() throws Exception {
        doConfigure(configuration);
    }

    /**
     * @see #store(Mail)
     */
    protected abstract void internalStore(Mail mc) throws MessagingException, IOException;


    /**
     * @see org.apache.james.services.MailRepository#remove(Mail)
     */
    public void remove(Mail mail) throws MessagingException {
        remove(mail.getName());
    }


    /**
     * @see org.apache.james.services.MailRepository#remove(Collection)
     */
    public void remove(Collection<Mail> mails) throws MessagingException {
        Iterator<Mail>delList = mails.iterator();
        while (delList.hasNext()) {
            remove(delList.next());
        }
    }

    /**
     * @see org.apache.james.services.MailRepository#remove(String)
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


    /**
     * @see #remove(String)
     */
    protected abstract void internalRemove(String key) throws MessagingException;


}
