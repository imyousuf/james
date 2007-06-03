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

package org.apache.james.experimental.imapserver.handler.commands;

import org.apache.commons.collections.ListUtils;
import org.apache.james.api.imap.ImapSessionState;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.mailboxmanager.Namespace;
import org.jmock.Mock;

public class CreateCommandTest extends AbstractCommandTest {

    public void testCreateNonFq() throws ProtocolException {
        final String userDefault = "#mock.user";
        final String fqMailboxName = userDefault + ".Test";

        setSessionState(ImapSessionState.AUTHENTICATED);
        setUpMailboxManager();
        setUpNamespace(userDefault);
        
        mockSession.expects(once()).method("unsolicitedResponses").will(returnValue(ListUtils.EMPTY_LIST));
        
        mockMailboxManager.expects(once()).method("createMailbox").with(
                eq(fqMailboxName));

        String response = handleRequest("1 CREATE Test\n");

        assertEquals("1 OK CREATE completed.\r\n", response);
    }

}
