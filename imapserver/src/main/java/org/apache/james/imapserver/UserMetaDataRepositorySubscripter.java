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

package org.apache.james.imapserver;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.user.UserMetaDataRespository;
import org.apache.james.api.user.UsersRepositoryException;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.SubscriptionException;
import org.apache.james.imap.store.Subscriber;

/**
 *
 */
public class UserMetaDataRepositorySubscripter implements Subscriber {

    public static final String META_DATA_KEY = "org.apache.james.IMAP_SUBSCRIPTIONS";

    private Log log = LogFactory.getLog(UserMetaDataRepositorySubscripter.class);

    private UserMetaDataRespository repository;
    private final Map<String,UserSubscription> userSubscriptionsByUser;

    public UserMetaDataRepositorySubscripter() {
        userSubscriptionsByUser = new HashMap<String, UserSubscription>();
    }

    @Resource(name = "userMetaDataRepository")
    public void setUserMetaDataRespository(UserMetaDataRespository repository) {
        this.repository = repository;
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.Subscriber#subscribe(org.apache.james.imap.mailbox.MailboxSession, java.lang.String)
     */
    public void subscribe(MailboxSession session, String mailbox) throws SubscriptionException {
        try {
            final UserSubscription subscription = getUserSubscription(session.getUser().getUserName());
            subscription.subscribe(mailbox);
        } catch (UsersRepositoryException e) {
            throw new SubscriptionException(HumanReadableText.GENERIC_SUBSCRIPTION_FAILURE, e);
        }
    }



    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.Subscriber#subscriptions(org.apache.james.imap.mailbox.MailboxSession)
     */
    @SuppressWarnings("unchecked")
    public Collection<String> subscriptions(MailboxSession session) throws SubscriptionException {
        try {
            final UserSubscription subscription = getUserSubscription(session.getUser().getUserName());
            final Collection<String> results = (Collection) subscription.subscriptions().clone();
            return results;
        } catch (UsersRepositoryException e) {
            throw new SubscriptionException(HumanReadableText.GENERIC_SUBSCRIPTION_FAILURE, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.store.Subscriber#unsubscribe(org.apache.james.imap.mailbox.MailboxSession, java.lang.String)
     */
    public void unsubscribe(MailboxSession session, String mailbox) throws SubscriptionException {
        try {
            final UserSubscription subscription = getUserSubscription(session.getUser().getUserName());
            subscription.unsubscribe(mailbox);
        } catch (UsersRepositoryException e) {
            throw new SubscriptionException(HumanReadableText.GENERIC_UNSUBSCRIPTION_FAILURE, e);
        }
    }

    private synchronized UserSubscription getUserSubscription(final String user) {
        UserSubscription subscription = (UserSubscription) userSubscriptionsByUser.get(user);
        if (subscription == null) {
            subscription = new UserSubscription(user, repository, log);
        }
        return subscription;
    }

    /**
     * Manages subscriptions for a user. Subscriptions are stored in a single
     * collection. This class synchronizes access for write operations.
     */
    private static final class UserSubscription {

        private final String user;
        private final UserMetaDataRespository repository;
        private Log log;

        public UserSubscription(final String user, final UserMetaDataRespository repository, Log log) {
            super();
            this.user = user;
            this.repository = repository;
        }

        public synchronized void subscribe(String mailbox) throws UsersRepositoryException {
            final HashSet<String> existingSubscriptions = subscriptions();
            if (!existingSubscriptions.contains(mailbox)) {
                final HashSet<String> newSubscriptions;
                if (existingSubscriptions == null) {
                    newSubscriptions = new HashSet<String>();
                } else {
                    existingSubscriptions.add(mailbox);
                    newSubscriptions = existingSubscriptions;
                }
                repository.setAttribute(user, newSubscriptions, META_DATA_KEY);
            }
        }

        public synchronized void unsubscribe(String mailbox) throws UsersRepositoryException {
            final HashSet<String> subscriptions = subscriptions();
            if (subscriptions.remove(mailbox)) {
                repository.setAttribute(user, subscriptions, META_DATA_KEY);
            }
        }

        @SuppressWarnings("unchecked")
        public HashSet<String> subscriptions() throws UsersRepositoryException {
            try {
                final HashSet<String> storedSubscriptions = (HashSet<String>) repository.getAttribute(user, META_DATA_KEY);
                final HashSet<String> results;
                if (storedSubscriptions == null) {
                    results = new HashSet<String>();
                } else {
                    results = storedSubscriptions;
                }
                return results;
            } catch (ClassCastException e) {
                log.error("ClassCastException during retrieval. Reseting subscriptions for user " + user);
                log.debug("HashSet expected but not retrieved.", e);
                return new HashSet<String>();
            }
        }
    }

}
