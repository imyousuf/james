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

import java.util.HashMap;
import java.util.Iterator;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.activity.Initializable;


/**
 * This class is used as a remote object to get the user manager object 
 * after successfully logged in!
 * 
 * @author <a href="mailto:buchi@email.com">Gabriel Bucher</a>
 */
public class UserManagerAdminImpl extends AbstractLogEnabled;
        implements UserManagerAdmin, Contextualizable, Composable, Configurable, Initializable {

    private static final String USER_MANAGER = "usermanager";
    private static final String ADMIN_ACCOUNTS = "admin_accounts";
    private static final String ADMIN_ACCOUNT = "account";
    private static final String ADMIN_ACCOUNT_LOGIN = "login";
    private static final String ADMIN_ACCOUNT_PASSWORD = "password";

    private Context context;

    private HashMap adminAccounts = new HashMap();
    private String userManagerBindname = "userManager";
    private UserManager userManager;

    
    public void contextualize(Context context)
            throws ContextException {
        this.context = context;    
    }

    public void compose(ComponentManager componentManager)
            throws ComponentException {
    }

    public void configure(Configuration configuration)
            throws ConfigurationException {
        this.userManagerBindname = configuration.getChild(this.USER_MANAGER).getValue(userManagerBindname);
        final Configuration adminAccountsConf = configuration.getChild(this.ADMIN_ACCOUNTS);
        final Configuration adminAccountConf[] = adminAccountsConf.getChildren(this.ADMIN_ACCOUNT);
        for (int i = 0; i < adminAccountConf.length; i++) {
            this.adminAccounts.put(adminAccountConf[i].getAttribute(this.ADMIN_ACCOUNT_LOGIN),
                                   adminAccountConf[i].getAttribute(this.ADMIN_ACCOUNT_PASSWORD));
        }
    }

    public void initialize()
            throws Exception {
        Iterator iterator = this.adminAccounts.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String)iterator.next();
        }

        this.userManager = (UserManagerImpl)this.context.get(this.userManagerBindname);
        if (this.userManager == null) {
            throw new ConfigurationException("No Remote UserManager object in the context found!");
        }
    }



     /**
      * This method is used to login to the user manager.
      * 
      * @param login    Admininistrator username
      * @param password Administrators password
      * @return if login successful the user manager remote object, otherwise null
      * @exception RemoteException
      */
    public UserManager login(String login, 
                             String password) 
            throws RemoteException {
        // check login, if it's ok return a UserManager object otherwise null
        if (password.equals(this.adminAccounts.get(login))) {
            getLogger().info("Administrator " + login + " logged in successful!");
            return this.userManager;
        }
        getLogger().warn("Login failed! " + login);
        return null;
    }

}

