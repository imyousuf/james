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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.core.MailImpl;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.User;
import org.apache.james.userrepository.DefaultJamesUser;
import org.apache.mailet.Mail;

/**
 * MailRepository wrapper to a MailboxManager <br />
 * This implementation should be considered as EXPERIMENTAL.
 * 
 * TODO examine for thread-safety
 */
public class MailboxManagerMailRepository extends AbstractMailRepository
        implements Configurable, Serviceable {

    private static final String PREFIX = "mailboxmanager://";
    
    // TODO extract delimiter from namespaces
    private static final String DL = ".";

    /**
     * used to map keys to uid and vice versa
     */
    private KeyBidiMap keyBidiMap = null;

    private MailboxGateKeeper mailboxGateKeeper;

    private MailboxManagerProvider mailboxManagerProvider;
    
    private String mailboxName;

    protected String addMessage(MimeMessage message) throws MessagingException {
        try {
            String myKey = getMailboxGateKeeper().getMailbox().store(message);
            return myKey;
        } catch (MailboxManagerException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * Does nothing
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws Exception {
        getLogger().debug("MailboxManagerMailRepository initialized");
    }

    /**
     * Stores a message in this repository.
     * 
     * @param mc
     *            the mail message to store
     * @throws MessagingException
     */
    public synchronized void store(Mail mc) throws MessagingException {
        MimeMessage message = mc.getMessage();
        String externalKey = mc.getName();

        getLogger().debug("store key:" + mc.getName());

        boolean wasLocked = true;
        try {
            getMailboxGateKeeper().use();

            synchronized (this) {
                wasLocked = getLock().isLocked(externalKey);
                if (!wasLocked) {
                    // If it wasn't locked, we want a lock during the store
                    lock(externalKey);
                }
            }

            // insert or update, don't call remove(key) because of locking
            if (getKeyBidiMap().containsExternalKey(externalKey)) {
                getLogger().info(
                        "remove message because of update Key:" + mc.getName());
                doRemove(externalKey);
            }
            String internalKey = addMessage(message);
            getKeyBidiMap().put(externalKey, internalKey);

            getLogger().info(
                    "message stored: externalKey: " + externalKey
                            + " internalKey:" + internalKey);
        } finally {

            if (!wasLocked) {
                // If it wasn't locked, we need to unlock now
                unlock(externalKey);
                synchronized (this) {
                    notify();
                }
            }
            getMailboxGateKeeper().free();
        }
    }

    /**
     * lazy loads UidToKeyBidiMap
     * 
     * @return
     */
    protected KeyBidiMap getKeyBidiMap() {
        if (keyBidiMap == null) {
            keyBidiMap = new KeyBidiMapImpl();
        }
        return keyBidiMap;
    }

    /**
     * Used for testing
     * 
     * @param keyBidiMap
     */
    void setKeyBidiMap(KeyBidiMap keyBidiMap) {
        this.keyBidiMap = keyBidiMap;
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
        getLogger().info("retrieve " + key);
        MimeMessage mm = getMessageFromInbox(key);
        if (mm == null)
            return null;
        Mail mail = new MailImpl(mm);
        mail.setName(key);
        return mail;
    }

    /**
     * Removes a message identified by key.
     * 
     * @param key
     *            the key of the message to be removed from the repository
     */
    public synchronized void remove(String key) throws MessagingException {
        getLogger().info(" remove key:" + key);
        doLockedRemove(key, true);
    }

    protected void doLockedRemove(String key, boolean expunge)
            throws MessagingException {
        if (lock(key)) {
            try {
                doRemove(key);
            } finally {
                unlock(key);
            }
        } else {
            getLogger().info("could not optain lock to remove key:" + key);
            throw new MessagingException("could not optain lock for remove");
        }
    }

    protected void doRemove(String externalKey) throws MessagingException {
        try {
            getMailboxGateKeeper().use();
            if (getKeyBidiMap().containsExternalKey(externalKey)) {
                String internalKey = getKeyBidiMap().getByExternalKey(
                        externalKey);
                getMailboxGateKeeper().getMailbox().remove(internalKey);
                getKeyBidiMap().removeByExternalKey(externalKey);
            }
        } catch (MailboxManagerException e) {
            throw new MessagingException(e.getMessage(), e);
        } finally {
            getMailboxGateKeeper().free();
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
    public Iterator list() throws MessagingException {
        getLogger().debug("MailboxManagerMailRepository list");
        try {
            getMailboxGateKeeper().use();
            Mailbox mailbox = getMailboxGateKeeper().getMailbox();

            // needed for retainAllListedAndAddedByKeys(String[], Collection)
            String[] externalKeysBefore = getKeyBidiMap().getExternalKeys();

            // get the messages
            Collection internalKeys = mailbox.list();
            Collection externalKeys = new ArrayList(internalKeys.size());
            for (Iterator iter = internalKeys.iterator(); iter.hasNext();) {
                String internalKey = (String) iter.next();
                String externalKey = getKeyBidiMap().getByInternalKey(
                        internalKey);

                if (externalKey == null) {
                    // generate new key
                    externalKey = "james-mailboxmanager:" + internalKey + ";"
                            + System.currentTimeMillis() + ";"
                            + getRandom().nextLong();
                    getKeyBidiMap().put(externalKey, internalKey);
                }
                externalKeys.add(externalKey);
                getLogger().debug(
                        "list: externalKey: " + externalKey + " internalKey:"
                                + internalKey);
            }
            // retain only listed keys, and keys added in the meantime (it would
            // be fatal to loose those)
            // I don't care about meanwhile removed, those will fail on next
            // access
            // it's a good idea to keep count of cached small
            getKeyBidiMap().retainAllListedAndAddedByExternalKeys(
                    externalKeysBefore, externalKeys);
            return externalKeys.iterator();
        } catch (MailboxManagerException e) {
            throw new MessagingException(e.getMessage(), e);
        } finally {
            getMailboxGateKeeper().free();
        }
    }

    private MimeMessage getMessageFromInbox(String externalKey)
            throws MessagingException {
        String internalKey=getKeyBidiMap().getByExternalKey(externalKey);
        MimeMessage mimeMessage = null;
        try {
            getMailboxGateKeeper().use();
            mimeMessage = getMailboxGateKeeper().getMailbox().retrieve(internalKey);
        } catch (MailboxManagerException e) {
            throw new MessagingException(e.getMessage(), e);
        } finally {
            getMailboxGateKeeper().free();
        }

        return mimeMessage;
    }

    public void remove(Collection mails) throws MessagingException {
        getLogger().debug("Remove by Collection KEYS:" + mails);
        try {
            getMailboxGateKeeper().use();
            for (Iterator iter = mails.iterator(); iter.hasNext();) {
                Mail mail = (Mail) iter.next();
                doRemove(mail.getName());
            }
        } finally {
            getMailboxGateKeeper().free();
        }

    }

    protected MailboxGateKeeper getMailboxGateKeeper() {
        if (mailboxGateKeeper == null) {
            mailboxGateKeeper = new MailboxGateKeeper();
        }
        return mailboxGateKeeper;
    }

    class MailboxGateKeeper {
        int open = 0;

        MailboxSession mailboxSession = null;

        synchronized void use() {
            open++;
        }

        synchronized void free() {
            if (open < 1) {
                throw new RuntimeException("use<1 !");
            }
            open--;
            if (open < 1) {
                if (open == 0) {
                    if (mailboxSession != null) {
                        try {
                            mailboxSession.close();
                        } catch (MailboxManagerException e) {
                            getLogger().error("error closing Mailbox", e);
                        }
                        mailboxSession=null;
                    }
                } else {
                    throw new RuntimeException("use<0 !");
                }
            }
        }

        synchronized Mailbox getMailbox() throws MailboxManagerException,
                MessagingException {
            if (open < 1) {
                throw new RuntimeException("use<1 !");
            }
            if (mailboxSession == null) {
                mailboxSession = getMailboxManagerProvider().getMailboxSession(
                        null, mailboxName, true);
            }
            return mailboxSession;
        }
    }

    protected MailboxManagerProvider getMailboxManagerProvider() {
        return mailboxManagerProvider;
    }

    public void setMailboxManagerProvider(
            MailboxManagerProvider mailboxManagerProvider) {
        this.mailboxManagerProvider = mailboxManagerProvider;
    }

    public void configure(Configuration conf) throws ConfigurationException {

        // fetch user name
        String destinationURL = conf.getAttribute("destinationURL");
        String postfix = conf.getAttribute("postfix", null);
        boolean translateDelimiter=conf.getAttributeAsBoolean("translateDelimiters",false);

        // transform the URL
        String name = destinationURL;
        
        // remove protocol prefix
        if (!name.startsWith(PREFIX)) {
            throw new ConfigurationException("url has to start with "+PREFIX);
        }
        name=name.substring(PREFIX.length());                
        
        
        // translate delimiter
        if (translateDelimiter) {
            // remove trailing /
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            name=name.replaceAll("/", DL);
        }
        
        // append postfix
        if (postfix!=null) {
            name += postfix;
        }
        
        getLogger().info(
                "Configured for mailbox: '" + name + "' URL: '"
                        + destinationURL + "' translateDelimiter: "+translateDelimiter);
        setMailboxName(name);
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        MailboxManagerProvider mailboxManagerProvider = (MailboxManagerProvider) serviceManager
                .lookup("org.apache.james.mailboxmanager.manager.MailboxManagerProvider");
        getLogger().debug(
                "MailboxManagerMailRepository uses service "
                        + mailboxManagerProvider);
        setMailboxManagerProvider(mailboxManagerProvider);
    }

    protected Log getLogger() {
        if (log == null) {
            log = new SimpleLog("MailboxManagerMailRepository");
        }
        return log;
    }

    String getMailboxName() {
        return mailboxName;
    }

    public void setMailboxName(String mailboxName) {
        this.mailboxName = mailboxName;
    }

}
