/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.userrepository;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.james.services.JamesUser;
import org.apache.mailet.MailAddress;

/**
 * Implementation of User Interface.
 *
 *
 * @version $Revision: 1.11 $
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
