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


package org.apache.james.util;


/**
 * A set of debugging utilities.
 */
public final class Assert
{
    public static final boolean ON = true;

    // Can't instantiate.
    private Assert()
    {
    };

    /**
     * Checks the supplied boolean expression, throwing an AssertionException if false;
     */
    public static void isTrue( boolean expression )
    {
        if ( !expression ) {
            throw new RuntimeException( "Assertion Failed." );
        }
    }

    /**
     * Fails with an assertion exception.
     */
    public static void fail()
    {
        throw new RuntimeException( "Assertion error - should not reach here." );
    }

    /**
     * Fails, indicating not-yet-implemented features.
     */
    public static void notImplemented()
    {
        throw new RuntimeException( "Not implemented" );
    }

}
