/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.mailet;

/**
 * Defines a general exception a mailet can throw when it encounters difficulty.
 *
 * @version 1.0.0, 24/04/1999
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class MailetException extends Exception {
    private Throwable rootCause = null;

    /**
     * Constructs a new mailet exception.
     */
    public MailetException() {
        super();
    }

    /**
     * Constructs a new mailet exception with the specified message.
     */
    public MailetException(String message) {
        super(message);
    }

    /**
     * Constructs a new mailet exception when the mailet needs to throw
     * an exception and include a message about the "root cause" exception
     * that interfered with its normal operation, including a description
     * message.
     */
    public MailetException(String message, Throwable t) {
        super(message);
        rootCause = t;
    }

    /**
     * Constructs a new mailet exception when the mailet needs to throw
     * an exception and include a message about the "root cause" exception
     * that interfered with its normal operation.
     */
    public MailetException(Throwable t) {
        super();
        rootCause = t;
    }

    /**
     * Returns the exception that caused this mailet exception.
     */
    public Throwable getRootCause() {
        return rootCause;
    }
}
