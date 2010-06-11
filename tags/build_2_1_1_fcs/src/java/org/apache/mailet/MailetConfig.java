/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.mailet;

import java.util.Iterator;

/**
 * A mailet configuration object used by a mailet container used to pass information
 * to a mailet during initialization.
 * <p>
 * The configuration information contains initialization parameters, which are a set
 * of name/value pairs, and a MailetContext object, which gives the mailet information
 * about the server.
 *
 * @version 1.0.0, 24/04/1999
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public interface MailetConfig {

    /**
     * Returns a String containing the value of the named initialization
     * parameter, or null if the parameter does not exist.
     *
     * @param name - a String specifying the name of the initialization parameter
     * @return a String containing the value of the initialization parameter
     */
    String getInitParameter(String name);

    /**
     * Returns the names of the mailet's initialization parameters as an
     * Iterator of String objects, or an empty Iterator if the mailet has
     * no initialization parameters.
     *
     * @return an Iterator of String objects containing the names of the mailet's
     *      initialization parameters
     */
    Iterator getInitParameterNames();

    /**
     * Returns a reference to the MailetContext in which the mailet is
     * executing.
     *
     * @return a MailetContext object, used by the mailet to interact with its
     *      mailet container
     */
    MailetContext getMailetContext();

    /**
     * Returns the name of this mailet instance. The name may be provided via
     * server administration, assigned in the application deployment descriptor,
     * or for an unregistered (and thus unnamed) mailet instance it will be the
     * mailet's class name.
     *
     * @return the name of the mailet instance
     */
    String getMailetName();
}
