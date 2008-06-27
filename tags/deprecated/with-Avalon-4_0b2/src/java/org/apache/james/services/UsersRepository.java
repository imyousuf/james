/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.services;

import java.util.Iterator;

/**
 * Interface for a repository of users. A repository represents a logical
 * grouping of users, typically by common purpose. E.g. the users served by an
 * email server or the members of a mailing list.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public interface UsersRepository {

    String USER = "USER";

    /**
     * Adds a user to the repository with the specified attributes.  In current
     * implementations, the Object attributes is generally a String password.
     */
    void addUser(String name, Object attributes);

    /**
     * Gets the attribute for a user.  Not clear on behavior.
     */
    Object getAttributes(String name);

    /**
     * Removes a user from the repository
     */
    void removeUser(String name);

    /**
     * Returns whether or not this user is in the repository
     */
    boolean contains(String name);

    /**
     * Tests a user with the appropriate attributes.  In current implementations,
     * this typically means "check the password" where a String password is passed
     * as the Object attributes.
     */
    boolean test(String name, Object attributes);

    /**
     * Returns a count of the users in the repository.
     */
    int countUsers();

    /**
     * List users in repository.
     *
     * @returns Iterator over a collection of Strings, each being one user in the repository.
     */
    Iterator list();

}
