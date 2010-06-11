/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

public interface ImapConstants
{
    // Basic response types
    String OK = "OK";
    String NO = "NO";
    String BAD = "BAD";
    String BYE = "BYE";
    String UNTAGGED = "*";

    String SP = " ";
    String VERSION = "IMAP4rev1";
    String CAPABILITIES = "LITERAL+";

    String USER_NAMESPACE = "#mail";

    char HIERARCHY_DELIMITER_CHAR = '.';
    char NAMESPACE_PREFIX_CHAR = '#';
    String HIERARCHY_DELIMITER = String.valueOf( HIERARCHY_DELIMITER_CHAR );
    String NAMESPACE_PREFIX = String.valueOf( NAMESPACE_PREFIX_CHAR );

    String INBOX_NAME = "INBOX";
}
