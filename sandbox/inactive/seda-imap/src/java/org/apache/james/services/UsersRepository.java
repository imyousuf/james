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
     * 
     * @deprecated James 2.4 user should be added using username/password
     * because specific implementations of UsersRepository will support specific 
     * implementations of users object.
     */
    boolean addUser(User user);

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
    void addUser(String name, Object attributes);
    
    /**
     * Adds a user to the repository with the specified password
     * 
     * @param username the username of the user to be added
     * @param password the password of the user to add
     * @return true if succesful, false otherwise
     * 
     * @since James 2.3.0
     */
    boolean addUser(String username, String password);

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
     * @deprecated James 2.4 now caseSensitive is a property of the repository
     * implementations and the getUserByName will search according to this property.
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
     * 
     * @deprecated James 2.4 now caseSensitive is a property of the repository
     * implementations and the contains will search according to this property.
     */
    boolean containsCaseInsensitive(String name);

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
