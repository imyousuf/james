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
 * @author Charles Benett <charles@benett1.demon.co.uk>
 *
 * Last changed by: $Author: charlesb $ on $Date: 2001/05/16 14:00:31 $
 * $Revision: 1.1 $
 */
public interface UsersRepository {

    String USER = "USER";

    /**
     * Adds a user to the repository with the specified User object.
     *
     * @returns true if succesful, false otherwise
     * @since James 1.2.2
     */
    boolean addUser(User user);

    /**
     * Adds a user to the repository with the specified attributes.  In current
     * implementations, the Object attributes is generally a String password.
     */
    void addUser(String name, Object attributes);

    /**
     * Gets the attribute for a user.  Not clear on behavior.
     *
     * @deprecated As of James 1.2.2 . Use the {@link #getUserByName(String) getUserByName} method.
     */
    Object getAttributes(String name);


    /**
     * Get the user object with the specified user name.  Return null if no
     * such user.
     *
     * @since James 1.2.2
     */
    User getUserByName(String name);

    /**
     * Get the user object with the specified user name. Match user naems on
     * a case insensitive basis.  Return null if no such user.
     *
     * @since James 1.2.2
     */
    User getUserByNameCaseInsensitive(String name);

    /**
     * Returns the user name of the user matching name on an equalsIgnoreCase
     * basis. Returns null if no match.
     */
    String getRealName(String name);

    /**
     * Update the repository with the specified user object. A user object
     * with this username must already exist.
     *
     * @returns true if successful.
     */
    boolean updateUser(User user);

    /**
     * Removes a user from the repository
     */
    void removeUser(String name);

    /**
     * Returns whether or not this user is in the repository
     */
    boolean contains(String name);

    /**
     * Returns whether or not this user is in the repository. Names are
     * matched on a case insensitive basis.
     */
    boolean containsCaseInsensitive(String name);


    /**
     * Tests a user with the appropriate attributes.  In current implementations,
     * this typically means "check the password" where a String password is passed
     * as the Object attributes.
     *
     * @deprecated As of James 1.2.2, use {@link #test(String, String) test(String name, String password)}
     */
    boolean test(String name, Object attributes);

    /**
     * Test if user with name 'name' has password 'password'.
     *
     * @since James 1.2.2
     */
    boolean test(String name, String password);

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
