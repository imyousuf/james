/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.remotemanager;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * The UserManagerAdmin class is used to login to the James
 * Administration Interface.
 * 
 * @author <a href="mailto:buchi@email.com">Gabriel Bucher</a>
 */
public interface UserManagerAdmin extends Remote {

    /**
     * This method is used to login to James to admin the User.
     * 
     * @param login    admin username
     * @param password admin password
     * @return Remote UserManager object if login was successful otherwise null
     * @exception RemoteException
     */
    UserManager login(String login,
                      String password) 
            throws RemoteException;

}

