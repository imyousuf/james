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
     * @return if the operation was successfull
     */
    boolean addUser(String userName, String password);
    
    /**
     * Deletes a user from this mail server.
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Delete an existing user
     *
     * @param userName The name of the user being deleted
     * @return if the operation was successfull
     */
    boolean deleteUser(String userName);

    /**
     * Check if a user exists with the given name.
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Check for existing user name
     *
     * @param userName The name of the user
     * @return TRUE, if the user exists
     */
    boolean verifyExists(String userName);

    /**
     * Total count of existing users
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Total count of existing users
     *
     * @return Total count of existing users
     */
    long countUsers();

    /**
     * List the names of all users
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description List all existing users
     *
     * @return List of all user names
     */
    String[] listAllUsers();

    /**
     * Set a user's password
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Set a user's password
     *
     * @param userName The name of the user whose password will be changed
     * @param password The new password 
     * @return if the user has been found and the password was changed successfully
     */
    boolean setPassword(String userName, String password) throws UserManagementException;
    
    /**
     * Set a user's alias to whom all mail is forwarded to
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Set a user's alias to whom all mail is forwarded to
     *
     * @param userName The name of the user whose alias is set
     * @param aliasUserName The user becoming the new alias 
     * @return if the user has been found and the password was changed successfully
     */
    boolean setAlias(String userName, String aliasUserName) throws UserManagementException;
    
    /**
     * Removes a user's alias which terminates local mail forwarding
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Removes a user's alias which terminates local mail forwarding
     *
     * @param userName The name of the user whose alias is unset
     * @return if the user has been found and the alias was removed
     */
    boolean unsetAlias(String userName) throws UserManagementException;
    
    /**
     * Retrieves the user's alias, if set
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Retrieves the user's alias, if set
     *
     * @return User's alias, or NULL, if no alias is set
     */
    String getAlias(String userName) throws UserManagementException;

    /**
     * Set a user's forward email address to whom all mail is forwarded to
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Set a user's forward email address to whom all mail is forwarded to
     *
     * @param userName The name of the user whose forward is set
     * @param forwardEmailAddress The new forward email address  
     * @return if the user has been found and the password was changed successfully
     */
    boolean setForwardAddress(String userName, String forwardEmailAddress) throws UserManagementException;
    
    /**
     * Removes a user's forward email address which terminates remote mail forwarding
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Removes a user's forward email address which terminates remote mail forwarding
     *
     * @param userName The name of the user whose forward is unset
     * @return if the user has been found and the forward was removed
     */
    boolean unsetForwardAddress(String userName) throws UserManagementException;
    
    /**
     * Retrieves the user's forward, if set
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Retrieves the user's forward, if set
     *
     * @return User's forward email address, or NULL, if no forward is set
     */
    String getForwardAddress(String userName) throws UserManagementException;

}
