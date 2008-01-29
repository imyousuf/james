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

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.response.imap4rev1.server.LSubResponse;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class LSubProcessorTest extends MockObjectTestCase {


    LSubProcessor processor;
    Mock next;
    Mock provider;
    Mock responder;
    Mock result;
    Mock session;
    Mock command;
    Mock serverResponseFactory;
    
    protected void setUp() throws Exception {
        serverResponseFactory = mock(StatusResponseFactory.class);
        session = mock(ImapSession.class);
        command = mock(ImapCommand.class);
        next = mock(ImapProcessor.class);
        responder = mock(ImapProcessor.Responder.class);
        result = mock(ListResult.class);
        provider = mock(MailboxManagerProvider.class);
        processor = createProcessor((ImapProcessor) next.proxy(), 
                (MailboxManagerProvider) provider.proxy(), (StatusResponseFactory) serverResponseFactory.proxy());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    LSubProcessor createProcessor(ImapProcessor next, MailboxManagerProvider provider, StatusResponseFactory factory) {
        return new LSubProcessor(next, provider, factory);
    }

    LSubResponse createResponse(boolean noinferior, boolean noselect, boolean marked, 
            boolean unmarked, String hierarchyDelimiter, String mailboxName) {
        return new LSubResponse(noinferior, noselect, marked, unmarked, hierarchyDelimiter, mailboxName);
    }

    void setUpResult(String[] attributes, String hierarchyDelimiter, String name) {
        result.expects(once()).method("getAttributes").will(returnValue(attributes));
        result.expects(once()).method("getHierarchyDelimiter").will(returnValue(hierarchyDelimiter));
        result.expects(once()).method("getName").will(returnValue(name));
    }
    
    public void testNoInferiors() throws Exception {
        String[] attributes = {"\\noise", ImapConstants.NAME_ATTRIBUTE_NOINFERIORS, "\\bogus"};
        setUpResult(attributes, ".", "#INBOX");
        responder.expects(once()).method("respond").with(
                eq(createResponse(true, false, false, false, ".", "#INBOX")));
        processor.processResult((ImapProcessor.Responder) responder.proxy(), 
                false, 0, (ListResult) result.proxy());
    }
    
    public void testNoSelect() throws Exception {
        String[] attributes = {"\\noise", ImapConstants.NAME_ATTRIBUTE_NOSELECT, "\\bogus"};
        setUpResult(attributes, ".", "#INBOX");
        responder.expects(once()).method("respond").with(
                eq(createResponse(false, true, false, false, ".", "#INBOX")));
        processor.processResult((ImapProcessor.Responder) responder.proxy(), 
                false, 0, (ListResult) result.proxy());
    }
    
    public void testUnMarked() throws Exception {
        String[] attributes = {"\\noise", ImapConstants.NAME_ATTRIBUTE_UNMARKED, "\\bogus"};
        setUpResult(attributes, ".", "#INBOX");
        responder.expects(once()).method("respond").with(
                eq(createResponse(false, false, false, true, ".", "#INBOX")));
        processor.processResult((ImapProcessor.Responder) responder.proxy(), 
                false, 0, (ListResult) result.proxy());
    }
    
    public void testMarked() throws Exception {
        String[] attributes = {"\\noise", ImapConstants.NAME_ATTRIBUTE_MARKED, "\\bogus"};
        setUpResult(attributes, ".", "#INBOX");
        responder.expects(once()).method("respond").with(
                eq(createResponse(false, false, true, false, ".", "#INBOX")));
        processor.processResult((ImapProcessor.Responder) responder.proxy(), 
                false, 0, (ListResult) result.proxy());
    }
    
    public void testMarkedAndUnmarked() throws Exception {
        String[] attributes = {"\\noise", ImapConstants.NAME_ATTRIBUTE_MARKED, ImapConstants.NAME_ATTRIBUTE_UNMARKED, "\\bogus"};
        setUpResult(attributes, ".", "#INBOX");
        responder.expects(once()).method("respond").with(
                eq(createResponse(false, false, true, false, ".", "#INBOX")));
        processor.processResult((ImapProcessor.Responder) responder.proxy(), 
                false, 0, (ListResult) result.proxy());
    }
    
    public void testUnmarkedAndMarked() throws Exception {
        String[] attributes = {"\\noise", ImapConstants.NAME_ATTRIBUTE_UNMARKED, ImapConstants.NAME_ATTRIBUTE_MARKED, "\\bogus"};
        setUpResult(attributes, ".", "#INBOX");
        responder.expects(once()).method("respond").with(
                eq(createResponse(false, false, true, false, ".", "#INBOX")));
        processor.processResult((ImapProcessor.Responder) responder.proxy(), 
                false, 0, (ListResult) result.proxy());
    }
    
    public void testNoSelectUnmarkedAndMarked() throws Exception {
        String[] attributes = {"\\noise", ImapConstants.NAME_ATTRIBUTE_NOSELECT, ImapConstants.NAME_ATTRIBUTE_UNMARKED, ImapConstants.NAME_ATTRIBUTE_MARKED, "\\bogus"};
        setUpResult(attributes, ".", "#INBOX");
        responder.expects(once()).method("respond").with(
                eq(createResponse(false, true, false, false, ".", "#INBOX")));
        processor.processResult((ImapProcessor.Responder) responder.proxy(), 
                false, 0, (ListResult) result.proxy());
    }
    
    public void testUnmarkedAndMarkedNoSelect() throws Exception {
        String[] attributes = {"\\noise",  ImapConstants.NAME_ATTRIBUTE_UNMARKED, ImapConstants.NAME_ATTRIBUTE_MARKED, ImapConstants.NAME_ATTRIBUTE_NOSELECT, "\\bogus"};
        setUpResult(attributes, ".", "#INBOX");
        responder.expects(once()).method("respond").with(
                eq(createResponse(false, true, false, false, ".", "#INBOX")));
        processor.processResult((ImapProcessor.Responder) responder.proxy(), 
                false, 0, (ListResult) result.proxy());
    }
    
    public void testUnmarkedNoSelectAndMarked() throws Exception {
        String[] attributes = {"\\noise",  ImapConstants.NAME_ATTRIBUTE_UNMARKED, ImapConstants.NAME_ATTRIBUTE_NOSELECT, ImapConstants.NAME_ATTRIBUTE_MARKED, "\\bogus"};
        setUpResult(attributes, ".", "#INBOX");
        responder.expects(once()).method("respond").with(
                eq(createResponse(false, true, false, false, ".", "#INBOX")));
        processor.processResult((ImapProcessor.Responder) responder.proxy(), 
                false, 0, (ListResult) result.proxy());
    }
    
    public void testNoinferiorsUnmarkedNoSelectAndMarked() throws Exception {
        String[] attributes = {"\\noise",  ImapConstants.NAME_ATTRIBUTE_NOINFERIORS, ImapConstants.NAME_ATTRIBUTE_UNMARKED, ImapConstants.NAME_ATTRIBUTE_NOSELECT, ImapConstants.NAME_ATTRIBUTE_MARKED, "\\bogus"};
        setUpResult(attributes, ".", "#INBOX");
        responder.expects(once()).method("respond").with(
                eq(createResponse(true, true, false, false, ".", "#INBOX")));
        processor.processResult((ImapProcessor.Responder) responder.proxy(), 
                false, 0, (ListResult) result.proxy());
    }    
}
