/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.phoenix.Block;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;

/**
 * Provides Authentication State. 
 * Authenticates users.
 * Access Control for User Commands.
 *
 * Implementation is NNTP Specific. 
 * Would it be better to have a James Authentication Service ?
 *
 * Application could define and plugin their own authentication service.
 *
 * @author  Harmeet <harmeet@kodemuse.com> 
 */
public class AuthServiceImpl extends AbstractLogEnabled
    implements AuthService, Composable, Configurable, Block 
{
    protected boolean authRequired;
    protected UsersRepository repo;
    protected String user;
    protected String password;
    protected boolean userSet = false;
    protected boolean passwordSet = false;

    public boolean isAuthorized(String command) {
        boolean allowed = isAuthenticated();
        // some commads are authorized, even if the user is not authenticated
        allowed = allowed || command.equalsIgnoreCase("AUTHINFO");
        allowed = allowed || command.equalsIgnoreCase("AUTHINFO");
        allowed = allowed || command.equalsIgnoreCase("MODE");
        allowed = allowed || command.equalsIgnoreCase("QUIT");
        return allowed;
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        UsersStore usersStore = (UsersStore)componentManager.lookup(UsersStore.ROLE);
        repo = usersStore.getRepository("LocalUsers");
    }

    public void configure( Configuration configuration ) throws ConfigurationException {
        authRequired =
            configuration.getChild("authRequired").getValueAsBoolean(false);
        getLogger().debug("Auth required state is :" + authRequired);
    }

    public boolean isAuthenticated() {
        if ( authRequired ) {
            if  (userSet && passwordSet ) {
                return repo.test(user,password);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public void setUser(String user) {
        if (user != null && user.trim().length() != 0) {
            this.user = user;
            userSet = true;
        }
        this.password = null;
    }

    public void setPassword(String password) {
        if (password != null && password.trim().length() != 0) {
            this.password = password;
            passwordSet = true;
        }
    }
}
