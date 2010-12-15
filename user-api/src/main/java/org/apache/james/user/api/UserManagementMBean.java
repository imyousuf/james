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



package org.apache.james.user.api;

import java.util.List;

/**
 * Expose user account management functionality through JMX.
 * 
 */
public interface UserManagementMBean {

    /**
     * Adds a user to this mail server.
     *
     *
     * @param userName The name of the user being added
     * @param password The password of the user being added
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the operation was successful
     */
    boolean addUser(String userName, String password, String repositoryName);

    /**
     * Deletes a user from this mail server.
     *
     *
     * @param userName The name of the user being deleted
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the operation was successful
     */
    boolean deleteUser(String userName, String repositoryName);

    /**
     * Check if a user exists with the given name.
     *
     *
     * @param userName The name of the user
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return TRUE, if the user exists
     */
    boolean verifyExists(String userName, String repositoryName);

    /**
     * Total count of existing users
     *
     *
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return Total count of existing users
     */
    long countUsers(String repositoryName);

    /**
     * List the names of all users
     *
     *
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return List of all user names
     */
    String[] listAllUsers(String repositoryName);

    /**
     * Set a user's password
     *
     *
     * @param userName The name of the user whose password will be changed
     * @param password The new password 
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the user has been found and the password was changed successfully
     */
    boolean setPassword(String userName, String password, String repositoryName);

    /**
     * Removes a user's alias which terminates local mail forwarding
     *
     *
     * @param userName The name of the user whose alias is unset
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the user has been found and the alias was removed
     */
    @Deprecated
    boolean unsetAlias(String userName, String repositoryName);

    /**
     * Retrieves the user's alias, if set
     *
     *
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return User's alias, or NULL, if no alias is set
     */
    @Deprecated
    String getAlias(String userName, String repositoryName) ;

    /**
     * Removes a user's forward email address which terminates remote mail forwarding
     *
     *
     * @param userName The name of the user whose forward is unset
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return if the user has been found and the forward was removed
     */
    @Deprecated
    boolean unsetForwardAddress(String userName, String repositoryName);

    /**
     * Retrieves the user's forward, if set
     *
     *
     * @param userName The name of the user whose forward is set
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return User's forward email address, or NULL, if no forward is set
     */
    @Deprecated
    String getForwardAddress(String userName, String repositoryName);

    /**
     * Retrieves a list of the names of all available user repositories
     *
     *
     * @return List<String> of repository names
     */
    List<String> getUserRepositoryNames();
    
    /**
     * Return true if the UserRepository has VirtualHosting enabled
     * 
     * @param repositoryName The user repository, to which the operation should be applied. If NULL, the LocalUsers
     *        repository is used.
     * @return virtual
     */
    public boolean getVirtualHostingEnabled(String repositoryName);
}
