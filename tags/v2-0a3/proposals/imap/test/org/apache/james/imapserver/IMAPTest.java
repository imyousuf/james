/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import javax.mail.internet.InternetAddress;

public interface IMAPTest
{
    public int PORT = 143;
    public String HOST = "localhost";

    public String USER = "imapuser";
    public String PASSWORD = "password";
    public String FROM_ADDRESS = "sender@somewhere";
    public String TO_ADDRESS = USER + "@" + HOST;
    
}
