/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james;

import org.apache.mailet.MailetContextConstants;

/**
 * Assorted Constants for use in all James blocks
 * The Software Version, Software Name and Build Date are set by ant during
 * the build process.
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 *
 * @version This is $Revision: 1.8 $
 */
public interface Constants extends MailetContextConstants{

    /**
     * The version of James.
     */
    public static final String SOFTWARE_VERSION = "@@VERSION@@";

    /**
     * The name of the software (i.e. James).
     */
    public static final String SOFTWARE_NAME = "@@NAME@@";

    /**
     * Key used to store the component manager for
     * this James instance in a way accessible by
     * Avalon aware Mailets.
     */
    public static final String AVALON_COMPONENT_MANAGER = "AVALON_COMP_MGR";

}
