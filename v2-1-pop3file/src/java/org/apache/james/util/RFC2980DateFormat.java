/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util;

import java.util.Locale;

/**
 * A thread-safe date formatting class to produce dates formatted in accord with the 
 * specifications of section 3.2 of RFC 2980.
 *
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public class RFC2980DateFormat extends SynchronizedDateFormat {

    /**
     * Constructor for RFC2980DateFormat
     */
    public RFC2980DateFormat() {
        super("yyyyMMddHHmmss", Locale.ENGLISH);
    }
}
