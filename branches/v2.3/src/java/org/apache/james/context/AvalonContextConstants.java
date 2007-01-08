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

package org.apache.james.context;

/**
 * This class is a placeholder for Avalon Context keys.
 *
 * In order to decouple James from Phoenix, and to allow James
 * to run in any Avalon Framework container it is necessary that
 * James not depend on the BlockContext class from Phoenix, but
 * rather only on the Context interface.  This requires that we
 * look up context values directly, using String keys.  This
 * class stores the String keys that are used by James to
 * look up context values.
 * 
 * The lifetime of this class is expected to be limited.  At some
 * point in the near future the Avalon folks will make a decision
 * about how exactly to define, describe, and publish context
 * values.  At that point we can replace this temporary mechanism
 * with the Avalon mechanism.  Unfortunately right now that decision
 * is still unmade, so we need to use this class as a temporary
 * solution.
 */
public class AvalonContextConstants {

    /**
     * Private constructor to prevent instantiation or subclassing
     */
    private AvalonContextConstants() {}

    /**
     * The context key associated with the home directory of the application
     * being run.  The object returned on a 
     * context.get(AvalonContextConstants.APPLICATION_HOME) should be of
     * type <code>java.io.File</code> and should be the home directory
     * for the application (in our case, James)
     */
    public static final String APPLICATION_HOME = "app.home";
}
