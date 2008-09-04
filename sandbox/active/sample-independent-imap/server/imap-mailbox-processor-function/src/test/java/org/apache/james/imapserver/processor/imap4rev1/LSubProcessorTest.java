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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.request.imap4rev1.LsubRequest;
import org.apache.james.imap.message.response.imap4rev1.server.LSubResponse;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class LSubProcessorTest extends MockObjectTestCase {

    private static final String ROOT = "ROOT";
    private static final String PARENT = ROOT  + ImapConstants.HIERARCHY_DELIMITER + "PARENT";
    private static final String CHILD_ONE = PARENT + ImapConstants.HIERARCHY_DELIMITER + "CHILD_ONE";
    private static final String CHILD_TWO = PARENT + ImapConstants.HIERARCHY_DELIMITER + "CHILD_TWO";
    private static final String MAILBOX_C = "C.MAILBOX";
    private static final String MAILBOX_B = "B.MAILBOX";
    private static final String MAILBOX_A = "A.MAILBOX";
    private static final String USER = "A User";
    private static final String TAG = "TAG";
    LSubProcessor processor;
    Mock next;
    Mock provider;
    Mock manager;
    Mock responder;
    Mock result;
    Mock session;
    Mock command;
    Mock serverResponseFactory;
    Mock statusResponse;
    Collection subscriptions;
    ImapCommand imapCommand;
    private ImapProcessor.Responder responderImpl;
    
    protected void setUp() throws Exception {
        subscriptions = new ArrayList();
        serverResponseFactory = mock(StatusResponseFactory.class);
        session = mock(ImapSession.class);
        command = mock(ImapCommand.class);
        imapCommand = (ImapCommand) command.proxy();
        next = mock(ImapProcessor.class);
        responder = mock(ImapProcessor.Responder.class);
        result = mock(ListResult.class);
        provider = mock(MailboxManagerProvider.class);
        statusResponse = mock(StatusResponse.class);
        responderImpl = (ImapProcessor.Responder) responder.proxy();
        manager = mock(MailboxManager.class);
        processor = new LSubProcessor((ImapProcessor) next.proxy(), (MailboxManagerProvider) provider.proxy(), 
                (StatusResponseFactory) serverResponseFactory.proxy());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testHierarchy() throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        
        responder.expects(once()).method("respond")
            .with(eq(new LSubResponse("", ImapConstants.HIERARCHY_DELIMITER, true)));
        
        expectOk();
        
        LsubRequest request = new LsubRequest(imapCommand,"", "", TAG);
        processor.doProcess(request, (ImapSession) session.proxy(), TAG, imapCommand, 
                responderImpl);
        
    }
    
    public void testShouldRespondToRegexWithSubscribedMailboxes() throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        subscriptions.add(CHILD_ONE);
        subscriptions.add(CHILD_TWO);
        
        responder.expects(once()).method("respond")
            .with(eq(new LSubResponse(CHILD_ONE, ImapConstants.HIERARCHY_DELIMITER, false)));        
        
        responder.expects(once()).method("respond")
            .with(eq(new LSubResponse(CHILD_TWO, ImapConstants.HIERARCHY_DELIMITER, false)));  
        
        expectSubscriptions();
        expectOk();
        
        LsubRequest request = new LsubRequest(imapCommand,"", PARENT + ImapConstants.HIERARCHY_DELIMITER + "%", TAG);
        processor.doProcess(request, (ImapSession) session.proxy(), TAG, imapCommand, 
                responderImpl);
        
    }
    
    public void testShouldRespondNoSelectToRegexWithParentsOfSubscribedMailboxes() throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        subscriptions.add(CHILD_ONE);
        subscriptions.add(CHILD_TWO);
        
        responder.expects(once()).method("respond")
            .with(eq(new LSubResponse(PARENT, ImapConstants.HIERARCHY_DELIMITER, true)));         
        
        expectSubscriptions();
        expectOk();
        
        LsubRequest request = new LsubRequest(imapCommand,"", ROOT + ImapConstants.HIERARCHY_DELIMITER + "%", TAG);
        processor.doProcess(request, (ImapSession) session.proxy(), TAG, imapCommand, 
                responderImpl);
        
    }
    
    public void testShouldRespondSelectToRegexWithParentOfSubscribedMailboxesWhenParentSubscribed() throws Exception {
        subscriptions.add(MAILBOX_A);
        subscriptions.add(MAILBOX_B);
        subscriptions.add(MAILBOX_C);
        subscriptions.add(PARENT);
        subscriptions.add(CHILD_ONE);
        subscriptions.add(CHILD_TWO);
        
        responder.expects(once()).method("respond")
            .with(eq(new LSubResponse(PARENT, ImapConstants.HIERARCHY_DELIMITER, false)));         
        
        expectSubscriptions();
        expectOk();
        
        LsubRequest request = new LsubRequest(imapCommand,"", ROOT + ImapConstants.HIERARCHY_DELIMITER + "%", TAG);
        processor.doProcess(request, (ImapSession) session.proxy(), TAG, imapCommand, 
                responderImpl);
        
    }

    public void testSelectAll() throws Exception {
        subscriptions.add(MAILBOX_A);
        responder.expects(once()).method("respond")
            .with(eq(new LSubResponse(MAILBOX_A, ImapConstants.HIERARCHY_DELIMITER, false)));
        subscriptions.add(MAILBOX_B);
        responder.expects(once()).method("respond")
            .with(eq(new LSubResponse(MAILBOX_B, ImapConstants.HIERARCHY_DELIMITER, false)));
        subscriptions.add(MAILBOX_C);
        responder.expects(once()).method("respond")
            .with(eq(new LSubResponse(MAILBOX_C, ImapConstants.HIERARCHY_DELIMITER, false)));
        
        expectSubscriptions();
        expectOk();
        
        LsubRequest request = new LsubRequest(imapCommand,"", "*", TAG);
        processor.doProcess(request, (ImapSession) session.proxy(), TAG, imapCommand, 
                responderImpl);
        
    }
    
    private void expectOk() {
        StatusResponse response = (StatusResponse) statusResponse.proxy();
        serverResponseFactory.expects(once()).method("taggedOk")
            .with(eq(TAG), same(imapCommand), eq(HumanReadableTextKey.COMPLETED))
                .will(returnValue(response));
        responder.expects(once()).method("respond").with(same(response));
    }
    
    private void expectSubscriptions() {
        session.expects(once()).method("getAttribute")
        .with(eq(ImapSessionUtils.MAILBOX_MANAGER_ATTRIBUTE_SESSION_KEY))
            .will(returnValue(manager.proxy()));
        session.expects(once()).method("getAttribute")
            .with(eq(ImapSessionUtils.MAILBOX_USER_ATTRIBUTE_SESSION_KEY))
                .will(returnValue(USER));
        
        manager.expects(once()).method("subscriptions")
            .with(eq(USER))
                .will(returnValue(subscriptions));
    }
}
