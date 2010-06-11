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
 * Last changed by: $Author: charlesb $ on $Date: 2001/05/16 14:00:30 $
 * $Revision: 1.1 $
 */

public interface User {

    /**
     * Return the user name of this user
     */
    String getUserName();

    /**
     * Return true if pass matches password of this user.
     */
    boolean verifyPassword(String pass);

}
