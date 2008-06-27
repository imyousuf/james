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
 * @version $Revision: 1.4 $
 */

public interface JamesUser extends User {

    /**
     * Change password to pass. Return true if successful.
     *
     * @param pass the new password
     * @return true if successful, false otherwise
     */
    boolean setPassword(String pass);

    /**
     * Indicate if mail for this user should be forwarded to some other mail
     * server.
     *
     * @param forward whether email for this user should be forwarded
     */
    void setForwarding(boolean forward);

    /** 
     * Return true if mail for this user should be forwarded
     */
    boolean getForwarding();

    /**
     * <p>Set destination for forwading mail</p>
     * <p>TODO: Should we use a MailAddress?</p>
     *
     * @param address the forwarding address for this user
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
     * Return true if emails should be delivered locally to an alias.
     */
    boolean getAliasing();

    /**
     * Set local address to which email should be delivered.
     *
     * @return true if successful
     */
    boolean setAlias(String address);

    /**
     * Get local address to which mail should be delivered.
     */
    String getAlias();


}
