/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

public class NNTPException extends RuntimeException {
    private final Throwable t;
    public NNTPException(String msg) {
        super(msg);
        this.t = null;
    }
    public NNTPException(String msg,Throwable t) {
        super(msg+((t!=null)?": "+t.toString():""));
        this.t = t;
    }
    public NNTPException(Throwable t) {
        super(t.toString());
        this.t = t;
    }
}
