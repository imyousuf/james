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
import java.util.Date;
import java.util.Iterator;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
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
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.Namespace;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.FlaggedMailbox;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.mailboxmanager.mailbox.UidMailbox;
import org.apache.james.mailboxmanager.manager.GeneralManager;
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

    /**
     * used to map keys to uid and vice versa
     */
    private UidToKeyBidiMap uidToKeyBidiMap = null;

    private GeneralManager mailboxManager;

    private MailboxGateKeeper mailboxGateKeeper;

    private MailboxManagerProvider mailboxManagerProvider;

    private User user;

    protected long addUIDMessage(MimeMessage message) throws MessagingException {
        try {
            MessageResult mr = getMailboxGateKeeper().getMailbox().appendMessage(message,
                    new Date(), MessageResult.UID);
            return mr.getUid();
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
        String key = mc.getName();
        
        getLogger().debug("store key:" + mc.getName());
        if (!message.isSet(Flag.RECENT)) {
            getLogger().debug("Message didn't have RECENT flag");
            message.setFlag(Flag.RECENT, true);
        }

        boolean wasLocked = true;
        try {
            getMailboxGateKeeper().use();
            
            synchronized (this) {
                wasLocked = getLock().isLocked(key);
                if (!wasLocked) {
                    // If it wasn't locked, we want a lock during the store
                    lock(key);
                }
            }

            // insert or update, don't call remove(key) because of locking
            if (getUidToKeyBidiMap().containsKey(key)) {
                getLogger().info("remove message because of update Key:" + mc.getName());
                doRemove(key,true);
            }
            long uid = addUIDMessage(message);
            getUidToKeyBidiMap().put(key, uid);

            getLogger().info("message stored: UID: " + uid + " Key:" + mc.getName());
        } finally {

            if (!wasLocked) {
                // If it wasn't locked, we need to unlock now
                unlock(key);
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
        doLockedRemove(key,true);
    }

    protected void doLockedRemove(String key,boolean expunge) throws MessagingException {
        if (lock(key)) {
            try {
                doRemove(key,expunge);
            } finally {
                unlock(key);
            }
        } else {
            getLogger().info("could not optain lock to remove key:" + key);
            throw new MessagingException("could not optain lock for remove");
        }
    }
    protected void doRemove(String key,boolean expunge) throws MessagingException {
        try {
            getMailboxGateKeeper().use();
            if (getUidToKeyBidiMap().containsKey(key)) {
                long uid = getUidToKeyBidiMap().getByKey(key);
                getMailboxGateKeeper().getMailbox().setFlags(
                        new Flags(Flags.Flag.DELETED), true, false,
                        GeneralMessageSetImpl.oneUid(uid), null);
                getUidToKeyBidiMap().removeByKey(key);
                if (expunge) {
                    doExpunge();
                }
            }
        } catch (MailboxManagerException e) {
            throw new MessagingException(e.getMessage(), e);
        } finally {
            getMailboxGateKeeper().free();
        }
    }

    protected void doExpunge() {
        try {
            getLogger().debug("Expunge");
            getMailboxGateKeeper().getMailbox().expunge(
                    GeneralMessageSetImpl.all(), MessageResult.NOTHING);
        } catch (MailboxManagerException e) {
            getLogger().error("Error expunging mailbox",e);
        } catch (MessagingException e) {
            getLogger().error("Error expunging mailbox",e);
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
        getLogger().debug("UIDPlusFolder list");
        try {
            getMailboxGateKeeper().use();
            FlaggedMailbox mailbox = getMailboxGateKeeper().getMailbox();
            
            // needed for retainAllListedAndAddedByKeys(String[], Collection)
            String[] keysBefore = getUidToKeyBidiMap().getKeys();
            
            // get the messages
            MessageResult[] messageResults = mailbox.getMessages(
                    GeneralMessageSetImpl.all(), MessageResult.UID
                            + MessageResult.FLAGS);
            Collection keys = new ArrayList(messageResults.length);
            for (int i = 0; i < messageResults.length; i++) {
                
                long uid = messageResults[i].getUid();
                
                if (!messageResults[i].getFlags().contains(Flags.Flag.DELETED)) {

                    long uidvalidity = ((UidMailbox) mailbox).getUidValidity();
                    // lookup uid

                    String key = getUidToKeyBidiMap().getByUid(uid);
                    if (key == null) {
                        // generate new key
                        key = "james-uid:" + uidvalidity + ";" + uid + ";"
                                + System.currentTimeMillis() + ";"
                                + getRandom().nextLong();
                        getUidToKeyBidiMap().put(key, uid);
                    }
                    keys.add(key);
                    getLogger().debug("list: UID: " + uid + " Key:" + key);
                } else {
                    getLogger().debug("don't list deleted UID:" + uid); 
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
        } catch (MailboxManagerException e) {
            throw new MessagingException(e.getMessage(), e);
        } finally {
            getMailboxGateKeeper().free();
        }
    }

    private MimeMessage getMessageFromInbox(String key)
            throws MessagingException {

        long uid = getUidToKeyBidiMap().getByKey(key);
        if (uid < 1) {
            return null;
        }
        MessageResult[] messageResults;
        try {
            getMailboxGateKeeper().use();
            messageResults = getMailboxGateKeeper().getMailbox().getMessages(
                    GeneralMessageSetImpl.oneUid(uid),
                    MessageResult.MIME_MESSAGE);
        } catch (MailboxManagerException e) {
            throw new MessagingException(e.getMessage(), e);
        } finally {
            getMailboxGateKeeper().free();
        }

        MimeMessage mm = null;
        if (messageResults.length == 1) {
            mm = messageResults[0].getMimeMessage();
        }
        getLogger().debug("getMessageFromInbox: UID: " + uid + " Key:" + key);
        if (mm == null) {
            getUidToKeyBidiMap().removeByKey(key);
            getLogger().info("Message not Found");
        }
        return mm;
    }

    public void remove(Collection mails) throws MessagingException {
        getLogger().debug("Remove by Collection KEYS:"+mails);
        try {
            getMailboxGateKeeper().use();
            for (Iterator iter = mails.iterator(); iter.hasNext();) {
                Mail mail = (Mail) iter.next();
                doRemove(mail.getName(), false);
            }
            doExpunge();
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

        FlaggedMailbox mailbox = null;

        synchronized void use() {
            open++;
        }

        synchronized void  free() {
            if (open < 1) {
                throw new RuntimeException("use<1 !");
            }
            open--;
            if (open < 1) {
                if (open == 0) {
                    if (mailbox != null) {
                        ((MailboxSession) mailbox).close();
                        mailbox=null;
                    }
                } else {
                    throw new RuntimeException("use<0 !");
                }
            }
        }

        synchronized FlaggedMailbox getMailbox()
                throws MailboxManagerException, MessagingException {
            if (open < 1) {
                throw new RuntimeException("use<1 !");
            }
            if (mailbox == null) {
                Namespace ns = getMailboxManager().getPersonalDefaultNamespace(
                        user);

                String inbox=ns.getName() + ns.getHierarchyDelimter()+ "INBOX";
                mailbox = getMailboxManager()
                        .getGenericImapMailboxSession(inbox);
            }
            return mailbox;
        }
    }

    /**
     * lazy loads a MailboxManager from MailboxManagerProvider
     * 
     */
    
    protected GeneralManager getMailboxManager() throws MessagingException,
            MailboxManagerException {
        if (mailboxManager == null) {
            if (user == null) {
                throw new MessagingException("user is null");
            }
            mailboxManager = getMailboxManagerProvider()
                    .getGeneralManagerInstance(user);
        }
        return mailboxManager;
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
        
        String destinationUrl = conf.getAttribute("destinationURL");
        if (destinationUrl.endsWith("/")) {
            destinationUrl = destinationUrl.substring(0, destinationUrl
                    .length() - 1);
        }
        String userName = destinationUrl.substring(destinationUrl
                .lastIndexOf('/') + 1);
        getLogger().info("Configured for user: '"+userName+"' URL: '"+destinationUrl+"'");
        setUser(new DefaultJamesUser(userName,"none"));
    }


    public void setUser(User user) {
        this.user=user;
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        MailboxManagerProvider mailboxManagerProvider =(MailboxManagerProvider) serviceManager.lookup("org.apache.james.mailboxmanager.manager.MailboxManagerProvider");
        getLogger().debug("MailboxManagerMailRepository uses service "+mailboxManagerProvider);
        setMailboxManagerProvider(mailboxManagerProvider);
    }
    
    
    protected Log getLogger() {
        if (log==null) {
            log=new SimpleLog("MailboxManagerMailRepository");
        }
        return log;
    }

}
