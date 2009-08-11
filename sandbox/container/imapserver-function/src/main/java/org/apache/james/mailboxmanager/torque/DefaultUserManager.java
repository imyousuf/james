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

package org.apache.james.mailboxmanager.torque;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.user.UserMetaDataRespository;
import org.apache.james.api.user.UserRepositoryException;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.mailbox.SubscriptionException;

/**
 * Stores subscription data in the user meta-data repository.
 */
public class DefaultUserManager implements UserManager {

    public static final String META_DATA_KEY
        ="org.apache.james.IMAP_SUBSCRIPTIONS";
    
    private Log log = LogFactory.getLog(DefaultUserManager.class);
    
    private final UserMetaDataRespository repository;
    private final Map userSubscriptionsByUser;
    
    private final UsersRepository usersRepository;
    
    public DefaultUserManager(final UserMetaDataRespository repository, final UsersRepository usersRepository) {
        super();
        this.repository = repository;
        userSubscriptionsByUser = new HashMap();
        this.usersRepository = usersRepository;
    }

    public void subscribe(String user, String mailbox)
            throws SubscriptionException {
        try {
            final UserSubscription subscription = getUserSubscription(user);
            subscription.subscribe(mailbox);
        } catch (UserRepositoryException e) {
            throw new SubscriptionException(HumanReadableText.GENERIC_SUBSCRIPTION_FAILURE, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<String> subscriptions(String user) throws SubscriptionException {
        try {
            final UserSubscription subscription = getUserSubscription(user);
            final Collection<String> results = (Collection) subscription.subscriptions().clone();
            return results;
        } catch (UserRepositoryException e) {
            throw new SubscriptionException(HumanReadableText.GENERIC_SUBSCRIPTION_FAILURE, e);
        }
    }

    public void unsubscribe(String user, String mailbox) throws SubscriptionException {
        try {
            final UserSubscription subscription = getUserSubscription(user);
            subscription.unsubscribe(mailbox);   
        } catch (UserRepositoryException e) {
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
     * Manages subscriptions for a user.
     * Subscriptions are stored in a single collection.
     * This class synchronizes access for write operations.
     */
    private static final class UserSubscription {
        
        private final String user;
        private final UserMetaDataRespository repository;
        private Log log;
        
        public UserSubscription(final String user, final UserMetaDataRespository repository,
                Log log) {
            super();
            this.user = user;
            this.repository = repository;
        }

        public synchronized void subscribe(String mailbox) throws UserRepositoryException {
            final HashSet<String> existingSubscriptions = subscriptions();
            if (!existingSubscriptions.contains(mailbox)) {
                final HashSet newSubscriptions;
                if (existingSubscriptions == null) {
                    newSubscriptions = new HashSet();
                } else {
                    existingSubscriptions.add(mailbox);
                    newSubscriptions = existingSubscriptions;
                }
                repository.setAttribute(user, newSubscriptions, META_DATA_KEY);
            }
        }
        
        public synchronized void unsubscribe(String mailbox) throws UserRepositoryException {
            final HashSet subscriptions = subscriptions();
            if (subscriptions.remove(mailbox)) {
                repository.setAttribute(user, subscriptions, META_DATA_KEY);
            }
        }
        
        public HashSet<String> subscriptions() throws UserRepositoryException {
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

    public boolean isAuthentic(String userid, String passwd) {
        return usersRepository.test(userid, passwd);
    }
}
