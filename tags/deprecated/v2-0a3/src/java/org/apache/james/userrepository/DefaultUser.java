/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
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
 * Last changed by: $Author: darrell $ on $Date: 2002/01/18 02:48:39 $
 * $Revision: 1.2 $
 */

public class DefaultUser implements User, Serializable {

    private String userName;
    private String hashedPassword;
    private String algorithm ;

    /**
     * Standard constructor.
     *
     * @param name the String name of this user
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
     * @returns the String of this users name
     */
    public String getUserName() {
	return userName;
    }

    /**
     *  Method to verify passwords. 
     *
     * @param pass the String that is claimed to be the password for this user
     * @returns true if the hash of pass with the current algorithm matches
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
     * @returns true if newPass successfuly hashed
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
     * @returns the String of the hashed Password
     */
    protected String getHashedPassword() {
	return hashedPassword;
    }

    /**
     * Method to access the hashing algorithm of the password.
     */
    protected String getHashAlgorithm() {
        return algorithm;
    }


}
