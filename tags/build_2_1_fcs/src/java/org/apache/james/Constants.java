/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james;

/**
 * Assorted Constants for use in all James blocks
 * The Software Version, Software Name and Build Date are set by ant during
 * the build process.
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 *
 * @version This is $Revision: 1.7 $
 */
public class Constants {

    /**
     * The version of James.
     */
    public static final String SOFTWARE_VERSION = "@@VERSION@@";

    /**
     * The name of the software (i.e. James).
     */
    public static final String SOFTWARE_NAME = "@@NAME@@";

    /**
     * Context key used to store the list of mail domains being
     * serviced by this James instance in the context.
     */
    public static final String SERVER_NAMES = "SERVER_NAMES";

    /**
     * Context key used to store the Mailet/SMTP "hello name" for this
     * James instance in the context.
     */
    public static final String HELLO_NAME = "HELLO_NAME";

    /**
     * Context key used to store the postmaster address for
     * this James instance in the context.
     */
    public static final String POSTMASTER = "POSTMASTER";

    /**
     * Key used to store the component manager for
     * this James instance in a way accessible by
     * Avalon aware Mailets.
     */
    public static final String AVALON_COMPONENT_MANAGER = "AVALON_COMP_MGR";

}
