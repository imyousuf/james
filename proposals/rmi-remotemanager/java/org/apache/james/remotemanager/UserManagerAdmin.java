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

