/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import org.apache.james.services.UsersRepository;

/**
 * @version 1.0.0, 31/03/2001
 * @author  Harmeet
 *
 * implements the authentication state. Should this be moved to a more common place ??
 * Should there be an authenication service, that manufactures and hands to the different
 * protocol servers AuthState objects.
 */
public class AuthState {
    private final boolean requiredAuth;
    private final UsersRepository repo;
    private String user;
    private String password;
    public AuthState(boolean requiredAuth,UsersRepository repo) {
        this.requiredAuth = requiredAuth;
        this.repo = repo;
    }

    public boolean isAuthenticated() {
        if ( requiredAuth )
            return repo.test(user,password);
        else
            return true;
    }
    public void setUser(String user) {
        this.user = user;
        this.password = null;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
