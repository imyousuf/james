/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.services;

/**
 * Interface for objects representing users.
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 *
 * @version $Revision: 1.5 $
 */

public interface User {

    /**
     * Return the user name of this user
     *
     * @return the user name for this user
     */
    String getUserName();

    /**
     * Return true if pass matches password of this user.
     *
     * @param pass the password to test
     * @return whether the password being tested is valid
     */
    boolean verifyPassword(String pass);

    /**
     * Sets new password from String. No checks made on guessability of
     * password.
     *
     * @param newPass the String that is the new password.
     * @return true if newPass successfully added
     */
    boolean setPassword(String newPass);
}
