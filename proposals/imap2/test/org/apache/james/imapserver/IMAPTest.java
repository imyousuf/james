/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;


public interface IMAPTest
{
    int PORT = 143;
    String HOST = "localhost";

    String USER = "imapuser";
    String PASSWORD = "password";
    String FROM_ADDRESS = "sender@localhost";
    String TO_ADDRESS = USER + "@" + HOST;

    int TIMEOUT = 30000;
}
