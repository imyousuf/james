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

package org.apache.james.experimental.imapserver;

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
    final char NAMESPACE_PREFIX_CHAR = '#';
    String HIERARCHY_DELIMITER = String.valueOf( HIERARCHY_DELIMITER_CHAR );
    final String NAMESPACE_PREFIX = String.valueOf( NAMESPACE_PREFIX_CHAR );

    String INBOX_NAME = "INBOX";
    public static final String STATUS_UNSEEN = "UNSEEN";
    public static final String STATUS_UIDVALIDITY = "UIDVALIDITY";
    public static final String STATUS_UIDNEXT = "UIDNEXT";
    public static final String STATUS_RECENT = "RECENT";
    public static final String STATUS_MESSAGES = "MESSAGES";
    public static final String UNSUBSCRIBE_COMMAND_NAME = "UNSUBSCRIBE";
    public static final String UID_COMMAND_NAME = "UID";
    public static final String SUBSCRIBE_COMMAND_NAME = "SUBSCRIBE";
    public static final String STORE_COMMAND_NAME = "STORE";
    public static final String STATUS_COMMAND_NAME = "STATUS";
    public static final String SELECT_COMMAND_NAME = "SELECT";
    public static final String SEARCH_COMMAND_NAME = "SEARCH";
    public static final String RENAME_COMMAND_NAME = "RENAME";
    public static final String NOOP_COMMAND_NAME = "NOOP";
    public static final String LSUB_COMMAND_NAME = "LSUB";
    public static final String LOGOUT_COMMAND_NAME = "LOGOUT";
    public static final String LOGIN_COMMAND_NAME = "LOGIN";
    public static final String LIST_COMMAND_NAME = "LIST";
    public static final String FETCH_COMMAND_NAME = "FETCH";
    public static final String EXPUNGE_COMMAND_NAME = "EXPUNGE";
    public static final String EXAMINE_COMMAND_NAME = "EXAMINE";
    public static final String DELETE_COMMAND_NAME = "DELETE";
    public static final String CREATE_COMMAND_NAME = "CREATE";
    public static final String COPY_COMMAND_NAME = "COPY";
    public static final String CLOSE_COMMAND_NAME = "CLOSE";
    public static final String CHECK_COMMAND_NAME = "CHECK";
    public static final String CAPABILITY_COMMAND_NAME = "CAPABILITY";
    public static final String AUTHENTICATE_COMMAND_NAME = "AUTHENTICATE";
    public static final String APPEND_COMMAND_NAME = "APPEND";
    public static final String CAPABILITY_RESPONSE = CAPABILITY_COMMAND_NAME + SP + VERSION + SP + CAPABILITIES;
    
}
