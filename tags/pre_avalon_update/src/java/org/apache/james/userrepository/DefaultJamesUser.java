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
 * Last changed by: $Author: darrell $ on $Date: 2002/01/18 02:48:39 $
 * $Revision: 1.2 $
 */

public class DefaultJamesUser 
        extends DefaultUser
        implements JamesUser, Initializable {

    private boolean forwarding;
    private MailAddress forwardingDestination;
    private boolean aliasing;
    private String alias;

    public DefaultJamesUser(String name, String alg) {
	super(name, alg);
    }

    public DefaultJamesUser(String name, String passwordHash, String hashAlg) {
        super(name, passwordHash, hashAlg);
    }


    /**
     * Call initialize when creating a new instance.
     */
    public void initialize() {
	forwarding = false;
	forwardingDestination = null;
	aliasing = false;
	alias = "";
    }

    public void setForwarding(boolean forward) {
	forwarding = forward;
    }

    public boolean getForwarding() {
	return forwarding;
    }

    
    public boolean setForwardingDestination(MailAddress address) {
	/* Some verification would be good */
	forwardingDestination = address;
	return true;
    }

    public MailAddress getForwardingDestination() {
	return forwardingDestination;
    }

    public void setAliasing(boolean alias) {
        aliasing = alias;
    }

    public boolean getAliasing() {
	return aliasing;
    }

    public boolean setAlias(String address) {
	/* Some verification would be good */
	alias = address;
	return true;
    }

    public String getAlias() {
	return alias;
    }
}
