/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.testing;

import java.lang.reflect.Method;

/**
 * A test class to test a single method call.
 */
class TestMethod {

    // Statistics

    /**
     * The total time taken by the test.
     */
    private int timeTaken;

    /**
     * The total number of method calls.
     */
    private int attempts;

    /**
     * The number of successful (that is, executed without generating an
     * exception) method calls.
     */
    private int success;

    /**
     * The name of the method being invoked.
     */
    final String name;

    /**
     * Constructor for the test.
     *
     * @param name the method to be invoked.
     */
    TestMethod(String name) {
        this.name = name;
    }

    /**
     * Invokes the test, registering an additional attempt and an
     * additional success if appropriate.  The total time taken is
     * recorded.
     *
     * @param obj An object of the class on which the method
     *            to be tested should be invoked.
     */
    void invoke(Object obj) throws Exception {
        Method m = obj.getClass().getMethod(name,new Class[0]);
        attempts++;
        long st = System.currentTimeMillis();
        try {
            m.invoke(obj,new Object[0]);
            success++;
        } finally {
            timeTaken = (int)(System.currentTimeMillis() - st);
        }
    }

    /**
     * Returns a representation of this object as a String.  The specific
     * representation is a report including the name of the called method,
     * the number of attempts, the number of successes, and the total time
     * taken.
     */
    public String toString() {
        StringBuffer theStringBuffer =
            new StringBuffer(128)
                    .append(name)
                    .append(", ")
                    .append(timeTaken)
                    .append(", ")
                    .append(success)
                    .append(", ")
                    .append(attempts);
        return theStringBuffer.toString();
    }
}
