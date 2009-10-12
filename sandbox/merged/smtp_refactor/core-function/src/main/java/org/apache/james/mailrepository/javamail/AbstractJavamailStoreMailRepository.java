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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailRepository;
import org.apache.mailet.Mail;

/**
 * MailRepository implementation to store mail in a Javamail store
 * 
 * This implementation should be considered as EXPERIMENTAL. <br>
 * <br>
 * TODO examine for thread-safety
 */

public abstract class AbstractJavamailStoreMailRepository extends
        AbstractLogEnabled implements MailRepository, StoreGateKeeperAware, FolderAdapterFactory, Configurable,
        Initializable,Serviceable {

    /**
     * Whether 'deep debugging' is turned on.
     */
    protected final static boolean DEEP_DEBUG = true;

    private static final String TYPE = "MAIL";

    /**
     * Assembled JavaMail destinationURL, only kept here for debugging via
     * getDestination()
     */
    private String destination;

    protected Logger log;

    /**
     * The underlaying Store can be used externaly via the StoreAware.getStore()
     * Method
     */
    private StoreGateKeeper storeGateKeeper = null;

    /** 
     * used internally to generate keys 
     */
    private static Random random;

    /**
     * this has not been tested yet, so it is not configurable. But it is likely
     * that there would be memory leaks
     */
    protected boolean cacheMessages = false;

    /**
     * A lock used to control access to repository elements, locking access
     * based on the key
     */
    private LockInterface lock;

    /**
     * inbox only accessable through a FolderGateKeeper
     */
    private FolderGateKeeper folderGateKeeper;

    /**
     * The directory james is running in
     */
    private File home;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager serviceManager) throws ServiceException {
        try {
            home = ((FileSystem) serviceManager.lookup(FileSystem.ROLE)).getBasedir();
        } catch (FileNotFoundException e) {
            throw new ServiceException(FileSystem.ROLE, "Cannot find the base directory of the application", e);
        }
    }

    /**
     * builds destination from attributes destinationURL and postfix.
     * at the moment james does not hand over additional parameters like postfix.
     * 
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     * 
     */
    public void configure(Configuration conf) throws ConfigurationException {
        log.debug("JavamailStoreMailRepository configure");
        destination = conf.getAttribute("destinationURL");
        log.debug("JavamailStoreMailRepository.destinationURL: " + destination);
        if (!destination.endsWith("/")) {
            destination += "/";
        }
        String postfix = conf.getAttribute("postfix", "");
        if (postfix.length() > 0) {
            if (postfix.startsWith("/")) {
                postfix = postfix.substring(1);
            }
            if (!postfix.endsWith("/")) {
                postfix += "/";
            }
            destination += postfix;

        }

        /*
         * Ugly hack to get the right folder to store the maildir I guess this
         * could be also configured in config.xml
         */
        final int pi = destination.indexOf(':');
        if (pi < 0) {
            throw new ConfigurationException("protocol prefix is missing "
                    + destination);
        }
        String withoutProtocol = destination.substring(pi);
        final String protocol = destination.substring(0, pi);
        try {
            if (!withoutProtocol.startsWith(":///")) {         
                withoutProtocol = "://"+  getDirAsUrl(home + "/" +withoutProtocol.substring(3));
            } else {
                withoutProtocol = "://" + getDirAsUrl("/" + withoutProtocol.substring(3));
            }
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid url: " + destination);
        }
    

        destination = protocol + withoutProtocol;
        log.debug("destination: " + destination);
        Properties mailSessionProps  = new Properties();

        // That seems to be deprecated
        // mailSessionProps.put("mail.store.maildir.autocreatedir", "true");
        Session mailSession = Session.getDefaultInstance(mailSessionProps);
        try {
            Store store=mailSession.getStore(new URLName(destination));
            store.connect();
            storeGateKeeper = new StoreGateKeeperImpl(store);
            storeGateKeeper.setFolderAdapterFactory(this);
        } catch (NoSuchProviderException e) {
            throw new ConfigurationException("cannot find store provider for "
                    + destination, e);
        } catch (MessagingException e) {
            throw new ConfigurationException("Error connecting store: "
                    + destination, e);
        }

        String checkType = conf.getAttribute("type");
        if (!checkType.equals(TYPE)) {
            String exceptionString = "Attempt to configure JavaMailStoreMailRepository as "
                    + checkType;
            log.warn(exceptionString);
            throw new ConfigurationException(exceptionString);
        }
        log.debug("JavaMailStoreMailRepository configured");
    }

    /**
     * Does nothing
     * @see Initializable#initialize()
     */
    public void initialize() throws Exception {
        log.debug("JavaMailStoreMailRepository initialized");
    }
    
    private String getDirAsUrl(String dir) throws MalformedURLException {
        File f = new File(dir); 
        return f.toURL().toString();
    }


    /**
     * gets the Lock and creates it, if not present. LockInterface offers functionality
     * of org.apache.james.util.Lock
     *
     * @return lock the LockInterface
     */
    protected LockInterface getLock() {
        if (lock==null) {
            lock = new LockAdapter();
        }
        return lock;
    }

    /**
     * possibility to replace Lock implementation. At the moment only used for testing 
     *
     * @param lock the LockInterface to use
     */
    void setLock(LockInterface lock) {
        this.lock=lock;
    }
    
    /**
     * used in unit tests 
     * 
     * @return the destination of the repository
     */
    String getDestination() {
        return destination;
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
     * Removes a specified message
     * 
     * @param mail
     *            the message to be removed from the repository
     * @throws MessagingException
     */
    public void remove(Mail mail) throws MessagingException {
        log.debug("UIDPlusFolder remove by Mail");
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
        log.debug("UIDPlusFolder remove by Collection " + mails.size());
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
     * offers the underlaying Store for external use
     * 
     * @return the Store 
     */
    public StoreGateKeeper getStore() {
        return storeGateKeeper;
    }

    /**
     * lazy-loads random 
     * 
     * @return random an instance of random
     */
    protected static synchronized Random getRandom() {
        if (random == null) {
            random = new Random();
        }
        return random;

    }
    
    /**
     * Set the Logger to use
     * 
     * @see org.apache.avalon.framework.logger.AbstractLogEnabled#enableLogging(Logger)
     */
    public void enableLogging(Logger log) {
        super.enableLogging(log);
        this.log=log;
        
    }
    
    /**
     * possibility to replace FolderGateKeeper implementation. Only used for
     * testing
     * 
     * @param gk the FolgerGateKeeper to use
     */
    void setFolderGateKeeper(FolderGateKeeper gk) {
        this.folderGateKeeper=gk;
        
    }
    
    /**
     * Lazy-load FolderGateKeeper with inbox folder. Inbox folder is created if
     * not present
     * @return FolderGateKeeper offering inbox folder for this mail repository
     */
    protected FolderGateKeeper getFolderGateKeeper() {
        if (folderGateKeeper == null) {
            try {
                folderGateKeeper = getStore().getFolder("INBOX");
                /*
                 * Check whether the folder exists, if not create it
                 */
                folderGateKeeper.use();
                if (folderGateKeeper.getFolder().exists() == false) {
                    folderGateKeeper.getFolder().create(Folder.HOLDS_MESSAGES);
                }
                folderGateKeeper.free();
            } catch (MessagingException e) {
                throw new RuntimeException(
                        "cannot retrieve inbox for this repository", e);
            } 
        }
        return folderGateKeeper;
    }
}
