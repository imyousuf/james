/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.mailet;

/**
 * A matcher configuration object used by a mailet container used to pass information
 * to a matcher during initialization.
 * <p>
 * The configuration information contains an initialization parameter,
 * which is set as a condition String, and a MailetContext object,
 * which gives the mailet information about the mailet container.
 *
 * @version 1.0.0, 24/04/1999
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public interface MatcherConfig {

    /**
     * The simple condition defined for this matcher, e.g., for
     * SenderIs=admin@localhost, this would return admin@localhost.
     *
     * @return a String containing the value of the initialization parameter
     */
    String getCondition();

    /**
     * Returns a reference to the MailetContext in which the matcher is executing
     *
     * @return a MailetContext object, used by the matcher to interact with its
     *      mailet container
     */
    MailetContext getMailetContext();

    /**
     * Returns the name of this matcher instance. The name may be provided via server
     * administration, assigned in the application deployment descriptor, or for
     * an unregistered (and thus unnamed) matcher instance it will be the matcher's
     * class name.
     *
     * @return the name of the matcher instance
     */
    String getMatcherName();
}
