/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.james.services;

import org.apache.mailet.MailAddress;

/**
 * Interface for objects representing users of an email/ messaging system.
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 *
 * @version $Revision: 1.4.4.2 $
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
