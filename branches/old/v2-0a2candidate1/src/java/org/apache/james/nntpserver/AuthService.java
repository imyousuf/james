/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

/**
 * Authenticates users and 
 * Access Control for User Commands.
 *
 * @author  Harmeet <harmeet@kodemuse.com> 
 */
public interface AuthService {
    String ROLE = "org.apache.james.nntpserver.AuthService";

    boolean isAuthenticated();
    void setUser(String userid);
    void setPassword(String password);
    boolean isAuthorized(String command);
}