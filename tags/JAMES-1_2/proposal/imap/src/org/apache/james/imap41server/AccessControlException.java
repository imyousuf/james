/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imap41server;

/**
 * Thrown when a user attempts to access something (e.g. a mailbox) for which
 * they do not have appropriate rights.
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version CVS $Revision: 1.1 $ 
 */
public class AccessControlException extends Exception {



    /**
     * Construct a new <code>AccessControlException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     */
    public AccessControlException(String message) {
        super(message);
    }


 
}
