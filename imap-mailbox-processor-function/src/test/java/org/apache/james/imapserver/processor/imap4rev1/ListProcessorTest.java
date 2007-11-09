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

package org.apache.james.imapserver.processor.imap4rev1;

import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.imap.message.response.imap4rev1.server.AbstractListingResponse;
import org.apache.james.imap.message.response.imap4rev1.server.ListResponse;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

public class ListProcessorTest extends AbstractTestListProcessor {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    AbstractListingProcessor createProcessor(ImapProcessor next, MailboxManagerProvider provider, StatusResponseFactory factory) {
        return new ListProcessor(next, provider, factory);
    }

    AbstractListingResponse createResponse(boolean noinferior, boolean noselect, boolean marked, 
            boolean unmarked, String hierarchyDelimiter, String mailboxName) {
        return new ListResponse(noinferior, noselect, marked, unmarked, hierarchyDelimiter, mailboxName);
    }

}