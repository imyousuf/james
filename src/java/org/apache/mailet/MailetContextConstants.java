/**
 * Constants.java
 *
 * Copyright (C) 07-Jan-2003 The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 *
 * Danny Angus
 */
package org.apache.mailet;
/**
 *
 * $Id: MailetContextConstants.java,v 1.3 2003/01/14 13:42:10 serge Exp $
 */
public interface MailetContextConstants {
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


}
