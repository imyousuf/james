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

