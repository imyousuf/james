/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.mailet;

import javax.mail.MessagingException;

/**
 * Defines a general exception a mailet can throw when it encounters difficulty.
 *
 * @version 1.0.0, 24/04/1999
 */
public class MailetException extends MessagingException {

    /**
     * Constructs a new mailet exception.
     */
    public MailetException() {
        super();
    }

    /**
     * Constructs a new mailet exception with the specified message.
     */
    public MailetException(String message) {
        super(message);
    }

    /**
     * Constructs a new mailet exception when the mailet needs to throw
     * an exception and include a message about the "root cause" exception
     * that interfered with its normal operation, including a description
     * message.
     */
    public MailetException(String message, Exception e) {
        super(message, e);
    }

}
