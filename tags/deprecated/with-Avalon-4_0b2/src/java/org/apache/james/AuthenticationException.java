/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james;

/**
 * Thrown when a user fails to authenticate either because their identity is
 * not recognised or because their credentials are wrong.
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class AuthenticationException 
    extends Exception {

    private boolean userNotKnown;
    private boolean badCredentials;

    /**
     * Construct a new <code>AuthenticationException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     */
    public AuthenticationException( final String message, 
                                    final boolean unknownUser,
                                    final boolean credentialsFailed ) {
        super(message);
        this.userNotKnown = unknownUser;
        this.badCredentials = credentialsFailed;
    }

    public boolean isUserNotKnown() {
        return userNotKnown;
    }

    public boolean isBadCredentials() {
        return badCredentials;
    }
}
