/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.userrepository;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.apache.james.services.User;

/**
 * Implementation of User Interface. Instances of this class do not allow
 * the password to be reset.
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 *
 * Last changed by: $Author: charlesb $ on $Date: 2001/05/23 09:21:32 $
 * $Revision: 1.2 $
 */

public class DefaultUser implements User, Serializable {

    private String userName;
    private byte[] hashedPassword;

    public DefaultUser(String name, String pass) {
	userName = name;
	hashedPassword = hashString(pass);
    }

    public String getUserName() {
	return userName;
    }

    public boolean verifyPassword(String pass) {
	byte[] hashGuess = hashString(pass);
	return Arrays.equals(hashedPassword, hashGuess);
    }

    protected boolean setPass(String newPass) {
     // Check that this is being called by a subclass not from package
	String rtClass = getClass().getName();
	if (rtClass.equals("org.apache.james.userrepository.DefaultUser")) {
	    throw new RuntimeException("Attempt to call setPassword in DefaultUSer");
	} else {
	    hashedPassword = hashString(newPass);
	    return true;
	}
    }

    protected byte[] getHashedPassword() {
	return hashedPassword;
    }

    private static byte[] hashString(String pass) {
	MessageDigest sha;
        try {
             sha = MessageDigest.getInstance("SHA");
	} catch (NoSuchAlgorithmException e) {
	    throw new RuntimeException("Can't hash passwords!" + e);
	}
	sha.update(pass.getBytes());
	return sha.digest();
    }


}
