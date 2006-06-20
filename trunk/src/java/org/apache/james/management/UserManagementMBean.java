/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.             *
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

package org.apache.james.management;

import java.util.List;

/**
 * Expose user account management functionality through JMX.
 * 
 * @phoenix:mx-topic name="UserAdministration"
 */
public interface UserManagementMBean {

    /**
     * Adds a user to this mail server.
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Add a new user
     *
     * @param userName The name of the user being added
     * @param password The password of the user being added
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the operation was successfull
     */
    boolean addUser(String userName, String password, String repositoryName) throws UserManagementException;

    /**
     * Deletes a user from this mail server.
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Delete an existing user
     *
     * @param userName The name of the user being deleted
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the operation was successfull
     */
    boolean deleteUser(String userName, String repositoryName) throws UserManagementException;

    /**
     * Check if a user exists with the given name.
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Check for existing user name
     *
     * @param userName The name of the user
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return TRUE, if the user exists
     */
    boolean verifyExists(String userName, String repositoryName) throws UserManagementException;

    /**
     * Total count of existing users
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Total count of existing users
     *
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return Total count of existing users
     */
    long countUsers(String repositoryName) throws UserManagementException;

    /**
     * List the names of all users
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description List all existing users
     *
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return List of all user names
     */
    String[] listAllUsers(String repositoryName) throws UserManagementException;

    /**
     * Set a user's password
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Set a user's password
     *
     * @param userName The name of the user whose password will be changed
     * @param password The new password 
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the user has been found and the password was changed successfully
     */
    boolean setPassword(String userName, String password, String repositoryName) throws UserManagementException;

    /**
     * Set a user's alias to whom all mail is forwarded to
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Set a user's alias to whom all mail is forwarded to
     *
     * @param userName The name of the user whose alias is set
     * @param aliasUserName The user becoming the new alias 
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the user has been found and the password was changed successfully
     */
    boolean setAlias(String userName, String aliasUserName, String repositoryName) throws UserManagementException;

    /**
     * Removes a user's alias which terminates local mail forwarding
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Removes a user's alias which terminates local mail forwarding
     *
     * @param userName The name of the user whose alias is unset
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the user has been found and the alias was removed
     */
    boolean unsetAlias(String userName, String repositoryName) throws UserManagementException;

    /**
     * Retrieves the user's alias, if set
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Retrieves the user's alias, if set
     *
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return User's alias, or NULL, if no alias is set
     */
    String getAlias(String userName, String repositoryName) throws UserManagementException;

    /**
     * Set a user's forward email address to whom all mail is forwarded to
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Set a user's forward email address to whom all mail is forwarded to
     *
     * @param userName The name of the user whose forward is set
     * @param forwardEmailAddress The new forward email address
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the user has been found and the password was changed successfully
     */
    boolean setForwardAddress(String userName, String forwardEmailAddress, String repositoryName) throws UserManagementException;

    /**
     * Removes a user's forward email address which terminates remote mail forwarding
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Removes a user's forward email address which terminates remote mail forwarding
     *
     * @param userName The name of the user whose forward is unset
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the user has been found and the forward was removed
     */
    boolean unsetForwardAddress(String userName, String repositoryName) throws UserManagementException;

    /**
     * Retrieves the user's forward, if set
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Retrieves the user's forward, if set
     *
     * @param userName The name of the user whose forward is set
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return User's forward email address, or NULL, if no forward is set
     */
    String getForwardAddress(String userName, String repositoryName) throws UserManagementException;

    /**
     * Retrieves a list of the names of all available user repositories
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Retrieves a list of the names of all available user repositories
     *
     * @return List<String> of repository names
     */
    public List getUserRepositoryNames();
}
