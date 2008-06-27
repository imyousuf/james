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
