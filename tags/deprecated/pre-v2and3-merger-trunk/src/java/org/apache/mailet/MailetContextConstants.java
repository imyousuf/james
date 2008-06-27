/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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
 * $Id: MailetContextConstants.java,v 1.6 2004/01/30 02:22:18 noel Exp $
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
