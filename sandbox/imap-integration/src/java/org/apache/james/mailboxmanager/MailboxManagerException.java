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

package org.apache.james.mailboxmanager;

public class MailboxManagerException extends Exception {

	private static final long serialVersionUID = -7034955921835169361L;

	private Exception cause;
    
    private String message;
    
    public MailboxManagerException(Exception e) {
        cause=e;
        message="MailboxException caused by "+cause;
    }

    public MailboxManagerException(String string) {
        message=string;
    }

    public Throwable getCause() {
        return cause;
    }

    public String getMessage() {
        return message;
    }
}
