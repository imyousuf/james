/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

/**
 * Authenticates users and provides
 * Access Control for User Commands.
 *
 * @author  Harmeet <harmeet@kodemuse.com> 
 */
public interface AuthService {

    /**
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.nntpserver.AuthService";

    /**
     * Check whether the service is authenticated
     *
     * @return true if the user and password have been set and are valid, false otherwise
     */
    boolean isAuthenticated();

    /**
     * Set the user id for this service
     *
     * @param userid the user id for this AuthService
     */
    void setUser(String userid);

    /**
     * Set the password for this service
     *
     * @param password the password for this AuthService
     */
    void setPassword(String password);

    /**
     * Check whether the service is authenticated
     *
     * @return true if the user is authenticated and is authorized to execute this command
     */
    boolean isAuthorized(String command);
}
