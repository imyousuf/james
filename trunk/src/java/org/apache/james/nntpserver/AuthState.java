/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

import org.apache.james.services.UsersRepository;

/**
 * implements the authentication state. 
 * Should this be moved to a more common place ??
 * Should there be an authenication service, that manufactures and hands to the different
 * protocol servers AuthState objects ?
 *
 * @version 1.0.0, 31/03/2001
 * @author  Harmeet <harmeet@kodemuse.com> 
*/
public class AuthState {
    private final boolean requiredAuth;
    private final UsersRepository repo;
    private String user;
    private String password;
    private boolean userSet = false;
    private boolean passwordSet = false;

    public AuthState(boolean requiredAuth,UsersRepository repo) {
        this.requiredAuth = requiredAuth;
        this.repo = repo;
    }

    public boolean isAuthenticated() {
        if ( requiredAuth ) {
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
