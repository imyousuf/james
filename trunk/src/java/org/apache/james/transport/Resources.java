/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport;

/**
 * A set of constants used inside the James transport.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Resources {

    //Already defined in Constants
    //public static final String SERVER_NAMES = "SERVER_NAMES";

    /**
     * Don't know what this is supposed to be. 
     *
     * @deprecated this is unused
     */
    public static final String USERS_MANAGER = "USERS_MANAGER";

    //Already defined in Constants
    //public static final String POSTMASTER = "POSTMASTER";

    /**
     * Don't know what this is supposed to be. 
     *
     * @deprecated this is unused
     */
    public static final String MAIL_SERVER = "MAIL_SERVER";

    /**
     * Don't know what this is supposed to be. 
     *
     * @deprecated this is unused
     */
    public static final String TRANSPORT = "TRANSPORT";

    /**
     * Don't know what this is supposed to be. 
     *
     * @deprecated this is unused
     */
    public static final String TMP_REPOSITORY = "TMP_REPOSITORY";

    /**
     * Key for looking up the MailetLoader
     */
    public static final String MAILET_LOADER = "MAILET_LOADER";

    /**
     * Key for looking up the MatchLoader
     */
    public static final String MATCH_LOADER = "MATCH_LOADER";

    /**
     * Private constructor to prevent instantiation of the class
     */
    private Resources() {}
}
