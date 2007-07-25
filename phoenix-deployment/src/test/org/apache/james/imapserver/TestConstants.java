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

package org.apache.james.imapserver;

public interface TestConstants
{
    final static String USER_NAME="tuser";
    final static String USER_MAILBOX_ROOT=ImapConstants.USER_NAMESPACE+"."+USER_NAME;
    final static String USER_INBOX=USER_MAILBOX_ROOT+".INBOX";
    
    final static String USER_PASSWORD="abc";
    final static String USER_REALNAME="Test User";
    
    final static String HOST_NAME="localhost";
    final static String HOST_ADDRESS="127.0.0.1";
    final static int HOST_PORT=10143;
    

    
}
