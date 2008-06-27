/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james;

/**
 * An interface to expose James management functionality through JMX.  At
 * the time of this writing, this interface is just an example.
 */
public interface JamesMBean {

    /**
     * Adds a user to this mail server.
     *
     * @param userName the name of the user being added
     * @param password the password of the user being added
     */
    boolean addUser(String userName, String password);
}
