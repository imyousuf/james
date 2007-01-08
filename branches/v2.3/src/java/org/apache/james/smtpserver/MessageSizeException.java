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

package org.apache.james.smtpserver;

import java.io.IOException;

/**
  * This exception is used to indicate when a new MimeMessage has exceeded
  * the maximum message size for the server, as configured in the conf file.
  *
  * @version 0.5.1
  */
public class MessageSizeException extends IOException {

    /**
     * Sole contructor for this class.  This constructor sets
     * the exception message to a fixed error message.
     */
    public MessageSizeException() {
        super("Message size exceeds fixed maximum message size.");
    }
}

