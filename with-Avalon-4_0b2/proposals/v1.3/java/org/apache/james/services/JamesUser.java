/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.services;

import org.apache.mailet.MailAddress;

/**
 * Interface for objects representing users of an email/ messaging system.
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 *
 * Last changed by: $Author: charlesb $ on $Date: 2001/05/22 12:03:22 $
 * $Revision: 1.2 $
 */

public interface JamesUser extends User {

    /**
     * Change password to pass. Return true if uccessful.
     */
    boolean setPassword(String pass);

    /**
     * Indicate if mail for this user should be forwarded to some other mail
     * server.
     */
    void setForwarding(boolean forward);

    /** 
     * Return true if mail for this user should be forwarded
     */
    boolean getForwarding();

    /**
     * Set destination for forwading mail
     * Should we use a MailAddress?
     */
    boolean setForwardingDestination(MailAddress address);

    /**
     * Return the destination to which email should be forwarded
     */
    MailAddress getForwardingDestination();

    /**
     * Indicate if mail received for this user should be delivered locally to
     * a different address.
     */
    void setAliasing(boolean alias);

    /**
     * Return true if emails should be dlivered locally to an alias.
     */
    boolean getAliasing();

    /**
     * Set local address to which email should be delivered.
     *
     * @returns true if successful
     */
    boolean setAlias(String address);

    /**
     * Get local address to which mail should be delivered.
     */
    String getAlias();


}
