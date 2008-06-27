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

package org.apache.james.imapserver;

/**
 * Thrown when a user attempts to access something (e.g. a mailbox) for which
 * they do not have appropriate rights.
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1  on 14 Dec 2000
 */
public class AccessControlException 
    extends Exception {

    /**
     * Construct a new <code>AccessControlException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     */
    public AccessControlException(String message) {
        super(message);
    }
}
