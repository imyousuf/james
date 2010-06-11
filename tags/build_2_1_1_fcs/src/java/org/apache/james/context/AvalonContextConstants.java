/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
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
