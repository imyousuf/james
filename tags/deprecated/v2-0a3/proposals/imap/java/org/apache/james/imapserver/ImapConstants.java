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
    String UNTAGGED = "*";

    String SP = " ";
    String VERSION = "IMAP4rev1";

    String AUTH_FAIL_MSG
            = "NO Command not authorized on this mailbox";
    String BAD_LISTRIGHTS_MSG
            = "BAD Command should be <tag> <LISTRIGHTS> <mailbox> <identifier>";
    String NO_NOTLOCAL_MSG
            = "NO Mailbox does not exist on this server";

    //mainly to switch on stack traces and catch responses;
    boolean DEEP_DEBUG = true;

    // Connection termination options
    int NORMAL_CLOSE = 0;
    int OK_BYE = 1;
    int UNTAGGED_BYE = 2;
    int TAGGED_NO = 3;
    int NO_BYE = 4;

    String LIST_WILD = "*";
    String LIST_WILD_FLAT = "%";
    char[] CTL = {};
    String[] ATOM_SPECIALS
            = {"(", ")", "{", " ", LIST_WILD, LIST_WILD_FLAT, };

    
}
