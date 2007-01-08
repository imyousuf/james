/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.nntpserver;

/**
 * Exception Wrapper, like javax.servlet.ServletException.
 * Purpose is to catch and wrap exceptions into unchecked NNTP specific. 
 * Protocol handler catches the exception and returns error info to client.
 * Error Information is obtained by calling 'getMessage'
 *
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
