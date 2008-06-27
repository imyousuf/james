/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

/**
 * Exception Wrapper, like javax.servlet.ServletException.
 * Purpose is to catch and wrap exceptions into unchecked NNTP specific. 
 * Protocol handler catches the exception and returns error info to client.
 * Error Information is obtained by calling 'getMessage'
 *
 * @author  Harmeet Bedi <harmeet@kodemuse.com>
 */
public class NNTPException extends RuntimeException {

    /**
     * The encapsulated Throwable
     */
    private final Throwable t;

    /**
     * Create an NNTPException with an error message and no
     * encapsulated <code>Throwable</code>
     *
     * @param msg the error message for this exception
     */
    public NNTPException(String msg) {
        super(msg);
        this.t = null;
    }

    /**
     * Create an NNTPException with an error message and an
     * encapsulated <code>Throwable</code>
     *
     * @param msg the error message for this exception
     * @param t the encapsulated <code>Throwable</code>
     */
    public NNTPException(String msg,Throwable t) {
        super(msg+((t!=null)?": "+t.toString():""));
        this.t = t;
    }

    /**
     * Create an NNTPException with an
     * encapsulated <code>Throwable</code>
     *
     * @param t the encapsulated <code>Throwable</code>
     */
    public NNTPException(Throwable t) {
        super(t.toString());
        this.t = t;
    }
}
