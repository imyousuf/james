/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.userrepository;

import java.io.Serializable;
import org.apache.james.services.User;

/**
 * Implementation of User Interface. Instances of this class do not allow
 * the password to be reset.
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 *
 * Last changed by: $Author: charlesb $ on $Date: 2001/05/16 14:00:35 $
 * $Revision: 1.1 $
 */

public class DefaultUser implements User, Serializable {

    private String userName;
    private String password;

    public DefaultUser(String name, String pass) {
	userName = name;
	password = pass;
    }

    public String getUserName() {
	return userName;
    }

    public boolean verifyPassword(String pass) {
	return pass.equals(password);
    }

    protected boolean setPass(String newPass) {
     // Check that this is being called by a subclass not from package
	String rtClass = getClass().getName();
	if (rtClass.equals("org.apache.james.userrepository.DefaultUser")) {
	    throw new RuntimeException("Attempt to call setPassword in DefaultUSer");
	} else {
	    password = newPass;
	    return true;
	}
    }

}
