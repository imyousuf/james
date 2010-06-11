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

package org.apache.james.userrepository;

import org.apache.james.security.DigestUtil;
import org.apache.james.services.User;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of User Interface. Instances of this class do not allow
 * the the user name to be reset.
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 *
 * @version CVS $Revision: 1.6.4.2 $
 */

public class DefaultUser implements User, Serializable {

    private String userName;
    private String hashedPassword;
    private String algorithm ;

    /**
     * Standard constructor.
     *
     * @param name the String name of this user
     * @param hashAlg the algorithm used to generate the hash of the password
     */
    public DefaultUser(String name, String hashAlg) {
        userName = name;
        algorithm = hashAlg;
    }

    /**
     * Constructor for repositories that are construcing user objects from
     * separate fields, e.g. databases.
     *
     * @param name the String name of this user
     * @param passwordHash the String hash of this users current password
     * @param hashAlg the String algorithm used to generate the hash of the
     * password
     */
    public DefaultUser(String name, String passwordHash, String hashAlg) {
        userName = name;
        hashedPassword = passwordHash;
        algorithm = hashAlg;
    }

    /**
     * Accessor for immutable name
     *
     * @return the String of this users name
     */
    public String getUserName() {
        return userName;
    }

    /**
     *  Method to verify passwords. 
     *
     * @param pass the String that is claimed to be the password for this user
     * @return true if the hash of pass with the current algorithm matches
     * the stored hash.
     */
    public boolean verifyPassword(String pass) {
        try {
            String hashGuess = DigestUtil.digestString(pass, algorithm);
            return hashedPassword.equals(hashGuess);
        } catch (NoSuchAlgorithmException nsae) {
        throw new RuntimeException("Security error: " + nsae);
    }
    }

    /**
     * Sets new password from String. No checks made on guessability of
     * password.
     *
     * @param newPass the String that is the new password.
     * @return true if newPass successfuly hashed
     */
    public boolean setPassword(String newPass) {
        try {
            hashedPassword = DigestUtil.digestString(newPass, algorithm);
            return true;
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("Security error: " + nsae);
        }
    }

    /**
     * Method to access hash of password
     *
     * @return the String of the hashed Password
     */
    protected String getHashedPassword() {
        return hashedPassword;
    }

    /**
     * Method to access the hashing algorithm of the password.
     *
     * @return the name of the hashing algorithm used for this user's password
     */
    protected String getHashAlgorithm() {
        return algorithm;
    }


}
