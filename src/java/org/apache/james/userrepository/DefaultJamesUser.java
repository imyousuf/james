/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.userrepository;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.james.services.JamesUser;
import org.apache.mailet.MailAddress;

/**
 * Implementation of User Interface.
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 *
 * @version $Revision: 1.6 $
 */

public class DefaultJamesUser 
        extends DefaultUser
        implements JamesUser, Initializable {

    /**
     * Whether forwarding is enabled for this user.
     */
    private boolean forwarding;

    /**
     * The mail address to which this user's email is forwarded.
     */
    private MailAddress forwardingDestination;

    /**
     * Is this user an alias for another username on the system.
     */
    private boolean aliasing;


    /**
     * The user name that this user name is aliasing.
     */
    private String alias;

    public DefaultJamesUser(String name, String alg) {
        super(name, alg);
    }

    public DefaultJamesUser(String name, String passwordHash, String hashAlg) {
        super(name, passwordHash, hashAlg);
    }


    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() {
        forwarding = false;
        forwardingDestination = null;
        aliasing = false;
        alias = "";
    }

    /**
     * Set whether mail to this user is to be forwarded to another
     * email address
     *
     * @param forward whether mail is forwarded
     */
    public void setForwarding(boolean forward) {
        forwarding = forward;
    }

    /**
     * Get whether mail to this user is to be forwarded to another
     * email address.
     *
     * @return forward whether mail is forwarded
     */
    public boolean getForwarding() {
        return forwarding;
    }

    
    /**
     * Set the destination address to which mail to this user
     * will be forwarded.
     *
     * @param address the forward-to address
     */
    public boolean setForwardingDestination(MailAddress address) {
        /* TODO: Some verification would be good */
        forwardingDestination = address;
        return true;
    }

    /**
     * Get the destination address to which mail to this user
     * will be forwarded.
     *
     * @return the forward-to address
     */
    public MailAddress getForwardingDestination() {
        return forwardingDestination;
    }

    /**
     * Set whether this user id is an alias.
     *
     * @param alias whether this id is an alias
     */
    public void setAliasing(boolean alias) {
        aliasing = alias;
    }

    /**
     * Get whether this user id is an alias.
     *
     * @return whether this id is an alias
     */
    public boolean getAliasing() {
        return aliasing;
    }

    /**
     * Set the user id for which this id is an alias.
     *
     * @param address the user id for which this id is an alias
     */
    public boolean setAlias(String address) {
        /* TODO: Some verification would be good */
        alias = address;
        return true;
    }

    /**
     * Get the user id for which this id is an alias.
     *
     * @return the user id for which this id is an alias
     */
    public String getAlias() {
        return alias;
    }
}
