/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
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
 * @version $Revision: 1.6.4.1 $
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
