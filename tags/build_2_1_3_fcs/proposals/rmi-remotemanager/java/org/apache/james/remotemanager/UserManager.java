/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.remotemanager;

import java.util.ArrayList;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * This interface specified all method which are possible to 
 * admin the Users.
 * 
 * @author <a href="buchi@email.com">Gabriel Bucher</a>
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
