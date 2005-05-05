/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.services;

import java.util.Iterator;

/**
 * Interface for a repository of users. A repository represents a logical
 * grouping of users, typically by common purpose. E.g. the users served by an
 * email server or the members of a mailing list.
 *
 *
 * @version $Revision$
 */
public interface UsersRepository {

    /**
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.services.UsersRepository";

    String USER = "USER";

    /**
     * Adds a user to the repository with the specified User object.
     *
     * @param user the user to be added
     *
     * @return true if succesful, false otherwise
     * @since James 1.2.2
     */
    boolean addUser(User user);

    /**
     * Adds a user to the repository with the specified attributes.  In current
     * implementations, the Object attributes is generally a String password.
     *
     * @param name the name of the user to be added
     * @param attributes see decription
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
     * @param name the name of the user to retrieve
     * @return the user being retrieved, null if the user doesn't exist
     *
     * @since James 1.2.2
     */
    User getUserByName(String name);

    /**
     * Get the user object with the specified user name. Match user naems on
     * a case insensitive basis.  Return null if no such user.
     *
     * @param name the name of the user to retrieve
     * @return the user being retrieved, null if the user doesn't exist
     *
     * @since James 1.2.2
     */
    User getUserByNameCaseInsensitive(String name);

    /**
     * Returns the user name of the user matching name on an equalsIgnoreCase
     * basis. Returns null if no match.
     *
     * @param name the name to case-correct
     * @return the case-correct name of the user, null if the user doesn't exist
     */
    String getRealName(String name);

    /**
     * Update the repository with the specified user object. A user object
     * with this username must already exist.
     *
     * @return true if successful.
     */
    boolean updateUser(User user);

    /**
     * Removes a user from the repository
     *
     * @param name the user to remove from the repository
     */
    void removeUser(String name);

    /**
     * Returns whether or not this user is in the repository
     *
     * @param name the name to check in the repository
     * @return whether the user is in the repository
     */
    boolean contains(String name);

    /**
     * Returns whether or not this user is in the repository. Names are
     * matched on a case insensitive basis.
     *
     * @param name the name to check in the repository
     * @return whether the user is in the repository
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
     * @param name the name of the user to be tested
     * @param password the password to be tested
     *
     * @return true if the test is successful, false if the user
     *              doesn't exist or if the password is incorrect
     *
     * @since James 1.2.2
     */
    boolean test(String name, String password);

    /**
     * Returns a count of the users in the repository.
     *
     * @return the number of users in the repository
     */
    int countUsers();

    /**
     * List users in repository.
     *
     * @return Iterator over a collection of Strings, each being one user in the repository.
     */
    Iterator list();

}
