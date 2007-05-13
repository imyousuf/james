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
package org.apache.james.experimental.imapserver.message;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.AppendRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.AuthenticateRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.CapabilityRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.CheckRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.CloseRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.CopyRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.CreateRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.DeleteRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.ExamineRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.ExpungeRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.FetchRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.ListRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.LoginRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.LogoutRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.LsubRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.NoopRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.RenameRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.SearchRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.SelectRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.StatusRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.StoreRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.SubscribeRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.UnsubscribeRequest;

/**
 * Naive, factory creates unpooled instances.
 */
public class BaseImapMessageFactory implements ImapMessageFactory {

    public ImapRequestMessage createErrorMessage(String message, String tag) {
        return new ErrorResponseMessage( message, tag );
    }

    public ImapRequestMessage createAppendMessage(ImapCommand command, String mailboxName, Flags flags, Date datetime, MimeMessage message, String tag) {
        return new AppendRequest(command, mailboxName, 
                flags, datetime, message, tag);
    }

    public ImapRequestMessage createAuthenticateMessage(ImapCommand command, String authType, String tag) {
        return new AuthenticateRequest(command, authType, tag);
    }

    public ImapRequestMessage createCapabilityMessage(ImapCommand command, String tag) {
        return new CapabilityRequest(command, tag);
    }

    public ImapRequestMessage createNoopMessage(ImapCommand command, String tag) {
        return new NoopRequest(command, tag);
    }

    public ImapRequestMessage createCloseMessage(ImapCommand command, String tag) {
        return new CloseRequest(command, tag);
    }

    public ImapRequestMessage createCopyMessage(ImapCommand command, IdRange[] idSet, String mailboxName, boolean useUids, String tag) {
        return new CopyRequest(command, idSet, mailboxName, useUids, tag);
    }

    public ImapRequestMessage createCreateMessage(ImapCommand command, String mailboxName, String tag) {
        return new CreateRequest(command, mailboxName, tag);
    }

    public ImapRequestMessage createDeleteMessage(ImapCommand command, String mailboxName, String tag) {
        return new DeleteRequest( command, mailboxName, tag );
    }

    public ImapRequestMessage createExamineMessage(ImapCommand command, String mailboxName, String tag) {
        return new ExamineRequest(command, mailboxName, tag);
    }

    public ImapRequestMessage createExpungeMessage(ImapCommand command, String tag) {
        return new ExpungeRequest(command, tag);
    }

    public ImapRequestMessage createFetchMessage(ImapCommand command, boolean useUids, IdRange[] idSet, FetchData fetch, String tag) {
        return new FetchRequest(command, useUids, idSet, fetch, tag);
    }

    public ImapRequestMessage createListMessage(ImapCommand command, String referenceName, String mailboxPattern, String tag) {
        return new ListRequest(command, referenceName, mailboxPattern, tag);
    }

    public ImapRequestMessage createLoginMessage(ImapCommand command, String userid, String password, String tag) {
        return new LoginRequest(command, userid, password, tag);
    }

    public ImapRequestMessage createLogoutMessage(ImapCommand command, String tag) {
        return new LogoutRequest(command, tag);
    }

    public ImapRequestMessage createLsubMessage(ImapCommand command, String referenceName, String mailboxPattern, String tag) {
        return new LsubRequest(command, referenceName, mailboxPattern, tag);
    }

    public ImapRequestMessage createRenameMessage(ImapCommand command, String existingName, String newName, String tag) {
        return new RenameRequest(command, existingName, newName, tag);
    }

    public ImapRequestMessage createSearchImapMessage(ImapCommand command, SearchTerm searchTerm, boolean useUids, String tag) {
        return new SearchRequest(command, searchTerm, useUids, tag);
    }

    public ImapRequestMessage createSelectMessage(ImapCommand command, String mailboxName, String tag) {
        return  new SelectRequest(command, mailboxName, tag);
    }

    public ImapRequestMessage createStatusMessage(ImapCommand command, String mailboxName, StatusDataItems statusDataItems, String tag) {
        return new StatusRequest(command, mailboxName, statusDataItems, tag);
    }

    public ImapRequestMessage createStoreMessage(ImapCommand command, IdRange[] idSet, StoreDirective directive, Flags flags, boolean useUids, String tag) {
        return new StoreRequest(command, idSet, directive, flags, useUids, tag);
    }

    public ImapRequestMessage createSubscribeMessage(ImapCommand command, String mailboxName, String tag) {
        return new SubscribeRequest(command, mailboxName, tag);
    }

    public ImapRequestMessage createUnsubscribeMessage(ImapCommand command, String mailboxName, String tag) {
        return new UnsubscribeRequest(command, mailboxName, tag);
    }

    public ImapRequestMessage createBadRequestMessage(String message) {
        return new BadResponseMessage(message);
    }

    public ImapRequestMessage createCheckMessage(ImapCommand command, String tag) {
        return new CheckRequest(command, tag);
    }  
    
    
}
