/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.smtpserver;

import java.io.*;

/**
  * This exceptions is used to indicate when a new MimeMessage has exceeded
  * the maximum message size for the server, as configured in the conf file.
  * @author Matthew Pangaro <mattp@lokitech.com>
  * @version 0.5.1
  */
public class MessageSizeException extends IOException {

    /** Default constructor that sets the message indicating message
        size error.
    */
    public MessageSizeException() {
        super("Message size exceeds fixed maximum message size.");
    }
}

