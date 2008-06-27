/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.services;

import org.apache.james.core.MailImpl;

import java.util.Iterator;

/**
 * Interface for a Repository to store Mails.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 */
public interface MailRepository {

    /**
     * Define a MAIL repository. MAILS are stored in the specified
     * destination.
     */
    String MAIL = "MAIL";


    /**
     * Stores a message in this repository. Shouldn't this return the key
     * under which it is stored?
     *
     * @param mc the mail message to store
     */
    void store(MailImpl mc);

    /**
     * List string keys of messages in repository.
     *
     * @return an <code>Iterator</code> over the list of keys in the repository
     *
     */
    Iterator list();

    /**
     * Retrieves a message given a key. At the moment, keys can be obtained
     * from list() in superinterface Store.Repository
     *
     * @param key the key of the message to retrieve
     * @return the mail corresponding to this key, null if none exists
     */
    MailImpl retrieve(String key);

    /**
     * Removes a specified message
     *
     * @param mail the message to be removed from the repository
     */
    void remove(MailImpl mail);

    /**
     * Removes a message identified by key.
     *
     * @param key the key of the message to be removed from the repository
     */
    void remove(String key);

    /**
     * Obtains a lock on a message identified by key
     *
     * @param key the key of the message to be locked
     *
     * @return true if successfully obtained the lock, false otherwise
     */
    boolean lock(String key);

    /**
     * Releases a lock on a message identified the key
     *
     * @param key the key of the message to be unlocked
     *
     * @return true if successfully released the lock, false otherwise
     */
    boolean unlock(String key);
}
