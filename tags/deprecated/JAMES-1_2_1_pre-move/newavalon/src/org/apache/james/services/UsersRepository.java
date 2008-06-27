/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.services;

import org.apache.avalon.services.*;
import org.apache.avalon.*;
//import org.apache.avalon.utils.*;
import java.util.Iterator;
//import java.io.*;

/**
 * Interface for a Repository to store users.
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public interface UsersRepository extends Service {

    public final static String USER = "USER";

    /**
     * Adds a user to the repository with the specified attributes.  In current
     * implementations, the Object attributes is generally a String password.
     */
    public void addUser(String name, Object attributes);

    /**
     * Gets the attribute for a user.  Not clear on behavior.
     */
    public Object getAttributes(String name);

    /**
     * Removes a user from the repository
     */
    public void removeUser(String name);

    /**
     * Returns whether or not this user is in the repository
     */
    public boolean contains(String name);

    /**
     * Tests a user with the appropriate attributes.  In current implementations,
     * this typically means "check the password" where a String password is passed
     * as the Object attributes.
     */
    public boolean test(String name, Object attributes);

    /**
     * Returns a count of the users in the repository.
     */
    public int countUsers();

    /**
     * List users in repository.
     *
     * @returns Iterator over a collection of Strings, each being one user in the repository.
     */
    public Iterator list();

}
