/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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
