/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james;

/**
 * Thrown when a user attempts to access something (e.g. a mailbox) for which
 * they do not have appropriate rights.
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1  on 14 Dec 2000
 */
public class AccessControlException 
    extends Exception {

    /**
     * Construct a new <code>AccessControlException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     */
    public AccessControlException(String message) {
        super(message);
    }
}
