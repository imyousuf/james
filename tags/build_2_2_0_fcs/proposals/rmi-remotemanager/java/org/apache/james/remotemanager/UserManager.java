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

package org.apache.james.remotemanager;

import java.util.ArrayList;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * This interface specified all method which are possible to 
 * admin the Users.
 * 
 */
public interface UserManager extends Remote {


    /**
     * Return the names of all available repositories.
     *
     * @return List of all repository names.
     * @exception RemoteException
     */
    ArrayList getRepositoryNames()
            throws RemoteException;

    /**
     * Set a repository from the list
     * 
     * @param repository name of the new repository
     * @return true if set repository was successful otherwise false.
     * @exception RemoteException
     */
    boolean setRepository(String repository)
            throws RemoteException;

    /**
     * add a new user.
     * 
     * @param username users name
     * @param password users password
     * @return true if user added successful otherwise false.
     * @exception RemoteException
     */
    boolean addUser(String username,
                    String password)
            throws RemoteException;
    /**
     * delete a user.
     * 
     * @param username users name
     * @return true if user deleted successful otherwise false
     * @exception RemoteException
     */
    boolean deleteUser(String username)
            throws RemoteException;
    /**
     * verify a user.
     * 
     * @param username users name
     * @return true if user exists otherwise false
     * @exception RemoteException
     */
    boolean verifyUser(String username)
            throws RemoteException;
    /**
     * list all users names.
     * 
     * @return list of all users
     * @exception RemoteException
     */
    ArrayList getUserList()
            throws RemoteException;
    /**
     * count all users.
     * 
     * @return 
     * @exception RemoteException
     */
    int getCountUsers()
            throws RemoteException;

    /**
     * reset password for a user.
     * 
     * @param username users name.
     * @param password new password
     * @return true if reset was successful otherwise false.
     * @exception RemoteException
     */
    boolean setPassword(String username,
                        String password)
            throws RemoteException;

    /**
     * set alias for users.
     * 
     * @param username users name.
     * @param alias    users alias
     * @return true if set alias was successful otherwise false.
     * @exception RemoteException
     */
    boolean setAlias(String username,
                     String alias)
            throws RemoteException;
    /**
     * unset alias.
     * 
     * @param username users name
     * @return true if unset alias was successful otherwise false
     * @exception RemoteException
     */
    boolean unsetAlias(String username)
            throws RemoteException;
    /**
     * check if alias is set for this user.
     * 
     * @param username users name
     * @return if alias is set you will get the alias otherwise null
     * @exception RemoteException
     */
    String checkAlias(String username)
            throws RemoteException;

    /**
     * set forward mailaddress for a user.
     * 
     * @param username users name.
     * @param forward  forward mailaddress
     * @return true if set forward was successful otherwise false
     * @exception RemoteException
     */
    boolean setForward(String username,
                       String forward)
            throws RemoteException;
    /**
     * unset forward to mailaddress.
     * 
     * @param username users name
     * @return true if unset forward was successful otherwise false.
     * @exception RemoteException
     */
    boolean unsetForward(String username)
            throws RemoteException;
    /**
     * check if forward is set.
     * 
     * @param username users name
     * @return if forward is set you will get the forwarding mailaddress
     *         otherweise null
     * @exception RemoteException
     */
    String checkForward(String username)
            throws RemoteException;

}
