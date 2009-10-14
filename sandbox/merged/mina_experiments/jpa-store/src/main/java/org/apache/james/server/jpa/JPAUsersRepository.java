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

package org.apache.james.server.jpa;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.user.User;
import org.apache.james.api.user.UsersRepository;

/**
 * Proof-of-concept repository using JPA.
 * TODO: Support managed contexts.
 * TODO: Use factory and support pooled contexts
 */
public class JPAUsersRepository implements UsersRepository {

    private static final Log LOGGER = LogFactory.getLog(JPAUsersRepository.class);

    private Log logger = LOGGER;

    private EntityManager entityManager;

    /**
     * Constructs repository with injection.
     * @param entityManager not null
     */
    public JPAUsersRepository(EntityManager entityManager) {
        super();
        this.entityManager = entityManager;
    }

    /**
     * Constructor for setting injection.
     */
    public JPAUsersRepository() {
        this(null);
    }

    /**
     * Gets current logger.
     * @return the logger
     */
    public final Log getLogger() {
        return logger;
    }

    /**
     * Setter injection for logging.
     * @param logger the logger to set
     */
    public final void setLogger(Log logger) {
        this.logger = logger;
    }

    /**
     * Gets entity manager.
     * @return the entityManager
     */
    public final EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Sets entity manager.
     * @param entityManager the entityManager to set
     */
    public final void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Adds a user to the repository with the specified User object.
     *
     * @param user the user to be added
     *
     * @return true if succesful, false otherwise
     * @since James 1.2.2
     * 
     * @deprecated James 2.4 user should be added using username/password
     * because specific implementations of UsersRepository will support specific 
     * implementations of users object.
     */
    public boolean addUser(User user) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a user to the repository with the specified attributes.  In current
     * implementations, the Object attributes is generally a String password.
     *
     * @param name the name of the user to be added
     * @param attributes see decription
     * 
     * @deprecated James 2.4 user is always added using username/password and
     * eventually modified by retrieving it later.
     */
    public void addUser(String name, Object attributes) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a user to the repository with the specified password
     * 
     * @param username the username of the user to be added
     * @param password the password of the user to add
     * @return true if succesful, false otherwise
     * 
     * @since James 2.3.0
     */
    public boolean addUser(String username, String password) {
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            JPAUser user = new JPAUser(username, password);
            entityManager.persist(user);
            transaction.commit();
            return true;
        } catch (PersistenceException e) {
            logger.debug("Failed to save user", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        }
        return false;
    }

    /**
     * Get the user object with the specified user name.  Return null if no
     * such user.
     *
     * @param name the name of the user to retrieve
     * @return the user being retrieved, null if the user doesn't exist
     *
     * @since James 1.2.2
     */
    public User getUserByName(String name) {
        return getJPAUserByName(name);
    }

    private JPAUser getJPAUserByName(String name) {
        try
        {
            return (JPAUser) entityManager.createQuery("SELECT user FROM User user WHERE user.name=?1")
                            .setParameter(1, name)
                            .getSingleResult();
        } catch (PersistenceException e) {
            logger.debug("Failed to find user", e);
            return null;
        }
    }

    /**
     * Get the user object with the specified user name. Match user naems on
     * a case insensitive basis.  Return null if no such user.
     *
     * @param name the name of the user to retrieve
     * @return the user being retrieved, null if the user doesn't exist
     *
     * @since James 1.2.2
     * @deprecated James 2.4 now caseSensitive is a property of the repository
     * implementations and the getUserByName will search according to this property.
     */
    public User getUserByNameCaseInsensitive(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the user name of the user matching name on an equalsIgnoreCase
     * basis. Returns null if no match.
     *
     * @param name the name to case-correct
     * @return the case-correct name of the user, null if the user doesn't exist
     */
    public String getRealName(String name) {
        return null;
    }

    /**
     * Update the repository with the specified user object. A user object
     * with this username must already exist.
     *
     * @return true if successful.
     */
    public boolean updateUser(User user) {
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            if (contains(user.getUserName())) {
                transaction.begin();
                entityManager.merge(user);
                transaction.commit();
            } else {
                logger.debug("User not found");
                return false;
            }
        } catch (PersistenceException e) {
            logger.debug("Failed to update user", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            return false;
        }
        return true;
    }

    /**
     * Removes a user from the repository
     *
     * @param name the user to remove from the repository
     */
    public void removeUser(String name) {
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            JPAUser user = getJPAUserByName(name);
            entityManager.remove(user);
            transaction.commit();
        } catch (PersistenceException e) {
            logger.debug("Failed to save user", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
        }
    }

    /**
     * Returns whether or not this user is in the repository
     *
     * @param name the name to check in the repository
     * @return whether the user is in the repository
     */
    public boolean contains(String name) {
        try
        {
            return ((Long) entityManager.createQuery("SELECT COUNT(user) FROM User user WHERE user.name=?1")
                            .setParameter(1, name)
                            .getSingleResult()).longValue() > 0;
        } catch (PersistenceException e) {
            logger.debug("Failed to find user", e);
            return false;
        }
    }

    /**
     * Returns whether or not this user is in the repository. Names are
     * matched on a case insensitive basis.
     *
     * @param name the name to check in the repository
     * @return whether the user is in the repository
     * 
     * @deprecated James 2.4 now caseSensitive is a property of the repository
     * implementations and the contains will search according to this property.
     */
    public boolean containsCaseInsensitive(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Test if user with name 'name' has password 'password'.
     *
     * @param name the name of the user to be tested
     * @param password the password to be tested
     *
     * @return true if the test is successful, false if the user
     *              doesn't exist or if the password is incorrect
     *
     * @since James 1.2.2
     */
    public boolean test(String name, String password) {
        final JPAUser user = getJPAUserByName(name);
        final boolean result;
        if (user == null)
        {
            result = false;
        }
        else
        {
            result = user.verifyPassword(password);
        }
        return result;
    }

    /**
     * Returns a count of the users in the repository.
     *
     * @return the number of users in the repository
     */
    public int countUsers() {
        try
        {
            return ((Long) entityManager.createQuery("SELECT COUNT(user) FROM User user")
                            .getSingleResult()).intValue();
        } catch (PersistenceException e) {
            logger.debug("Failed to find user", e);
            return 0;
        }
    }

    /**
     * List users in repository.
     *
     * @return Iterator over a collection of Strings, each being one user in the repository.
     */
    public Iterator list() {
        try
        {
            final List results = entityManager.createQuery("SELECT user FROM User user").getResultList();
            return new Iterator() {
                private final Iterator it = results.iterator();
                public boolean hasNext() {
                    return it.hasNext();
                }

                public Object next() {
                    return ((JPAUser)it.next()).getUserName();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } catch (PersistenceException e) {
            logger.debug("Failed to find user", e);
            return Collections.EMPTY_LIST.iterator();
        }
    }


}
