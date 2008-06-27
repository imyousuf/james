/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import org.apache.mailet.MailetContext;
import org.apache.mailet.MatcherConfig;

/**
 * Implements the configuration object for a Matcher.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class MatcherConfigImpl implements MatcherConfig {

    /**
     * A String representation of the value for the matching condition
     */
    private String condition;

    /**
     * The name of the Matcher
     */
    private String name;

    /**
     * The MailetContext associated with the Matcher configuration
     */
    private MailetContext context;

    /**
     * The simple condition defined for this matcher, e.g., for
     * SenderIs=admin@localhost, this would return admin@localhost.
     *
     * @return a String containing the value of the initialization parameter
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Set the simple condition defined for this matcher configuration.
     */
    public void setCondition(String newCondition) {
        condition = newCondition;
    }

    /**
     * Returns the name of this matcher instance. The name may be provided via server
     * administration, assigned in the application deployment descriptor, or for
     * an unregistered (and thus unnamed) matcher instance it will be the matcher's
     * class name.
     *
     * @return the name of the matcher instance
     */
    public String getMatcherName() {
        return name;
    }

    /**
     * Sets the name of this matcher instance.
     *
     * @param newName the name of the matcher instance
     */
    public void setMatcherName(String newName) {
        name = newName;
    }

    /**
     * Returns a reference to the MailetContext in which the matcher is executing
     *
     * @return a MailetContext object, used by the matcher to interact with its
     *      mailet container
     */
    public MailetContext getMailetContext() {
        return context;
    }

    /**
     * Sets a reference to the MailetContext in which the matcher is executing
     *
     * @param newContext a MailetContext object, used by the matcher to interact
     *      with its mailet container
     */
    public void setMailetContext(MailetContext newContext) {
        context = newContext;
    }
}
