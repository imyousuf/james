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
package org.apache.james.imap.message.request.base;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.message.FetchData;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.StatusDataItems;
import org.apache.james.imap.message.request.imap4rev1.AppendRequest;
import org.apache.james.imap.message.request.imap4rev1.AuthenticateRequest;
import org.apache.james.imap.message.request.imap4rev1.CapabilityRequest;
import org.apache.james.imap.message.request.imap4rev1.CheckRequest;
import org.apache.james.imap.message.request.imap4rev1.CloseRequest;
import org.apache.james.imap.message.request.imap4rev1.CopyRequest;
import org.apache.james.imap.message.request.imap4rev1.CreateRequest;
import org.apache.james.imap.message.request.imap4rev1.DeleteRequest;
import org.apache.james.imap.message.request.imap4rev1.ExamineRequest;
import org.apache.james.imap.message.request.imap4rev1.ExpungeRequest;
import org.apache.james.imap.message.request.imap4rev1.FetchRequest;
import org.apache.james.imap.message.request.imap4rev1.ListRequest;
import org.apache.james.imap.message.request.imap4rev1.LoginRequest;
import org.apache.james.imap.message.request.imap4rev1.LogoutRequest;
import org.apache.james.imap.message.request.imap4rev1.LsubRequest;
import org.apache.james.imap.message.request.imap4rev1.NoopRequest;
import org.apache.james.imap.message.request.imap4rev1.RenameRequest;
import org.apache.james.imap.message.request.imap4rev1.SearchRequest;
import org.apache.james.imap.message.request.imap4rev1.SelectRequest;
import org.apache.james.imap.message.request.imap4rev1.StatusRequest;
import org.apache.james.imap.message.request.imap4rev1.StoreRequest;
import org.apache.james.imap.message.request.imap4rev1.SubscribeRequest;
import org.apache.james.imap.message.request.imap4rev1.UnsubscribeRequest;
import org.apache.james.imap.message.response.imap4rev1.legacy.BadResponse;
import org.apache.james.imap.message.response.imap4rev1.legacy.ErrorResponse;

/**
 * Naive, factory creates unpooled instances.
 */
public class BaseImap4Rev1MessageFactory implements Imap4Rev1MessageFactory {

    public ImapMessage createErrorMessage(String message, String tag) {
        return new ErrorResponse( message, tag );
    }

    public ImapMessage createAppendMessage(ImapCommand command, String mailboxName, Flags flags, Date datetime, MimeMessage message, String tag) {
        return new AppendRequest(command, mailboxName, 
                flags, datetime, message, tag);
    }

    public ImapMessage createAuthenticateMessage(ImapCommand command, String authType, String tag) {
        return new AuthenticateRequest(command, authType, tag);
    }

    public ImapMessage createCapabilityMessage(ImapCommand command, String tag) {
        return new CapabilityRequest(command, tag);
    }

    public ImapMessage createNoopMessage(ImapCommand command, String tag) {
        return new NoopRequest(command, tag);
    }

    public ImapMessage createCloseMessage(ImapCommand command, String tag) {
        return new CloseRequest(command, tag);
    }

    public ImapMessage createCopyMessage(ImapCommand command, IdRange[] idSet, String mailboxName, boolean useUids, String tag) {
        return new CopyRequest(command, idSet, mailboxName, useUids, tag);
    }

    public ImapMessage createCreateMessage(ImapCommand command, String mailboxName, String tag) {
        return new CreateRequest(command, mailboxName, tag);
    }

    public ImapMessage createDeleteMessage(ImapCommand command, String mailboxName, String tag) {
        return new DeleteRequest( command, mailboxName, tag );
    }

    public ImapMessage createExamineMessage(ImapCommand command, String mailboxName, String tag) {
        return new ExamineRequest(command, mailboxName, tag);
    }

    public ImapMessage createExpungeMessage(ImapCommand command, String tag) {
        return new ExpungeRequest(command, tag);
    }

    public ImapMessage createFetchMessage(ImapCommand command, boolean useUids, IdRange[] idSet, FetchData fetch, String tag) {
        return new FetchRequest(command, useUids, idSet, fetch, tag);
    }

    public ImapMessage createListMessage(ImapCommand command, String referenceName, String mailboxPattern, String tag) {
        return new ListRequest(command, referenceName, mailboxPattern, tag);
    }

    public ImapMessage createLoginMessage(ImapCommand command, String userid, String password, String tag) {
        return new LoginRequest(command, userid, password, tag);
    }

    public ImapMessage createLogoutMessage(ImapCommand command, String tag) {
        return new LogoutRequest(command, tag);
    }

    public ImapMessage createLsubMessage(ImapCommand command, String referenceName, String mailboxPattern, String tag) {
        return new LsubRequest(command, referenceName, mailboxPattern, tag);
    }

    public ImapMessage createRenameMessage(ImapCommand command, String existingName, String newName, String tag) {
        return new RenameRequest(command, existingName, newName, tag);
    }

    public ImapMessage createSearchImapMessage(ImapCommand command, SearchTerm searchTerm, boolean useUids, String tag) {
        return new SearchRequest(command, searchTerm, useUids, tag);
    }

    public ImapMessage createSelectMessage(ImapCommand command, String mailboxName, String tag) {
        return  new SelectRequest(command, mailboxName, tag);
    }

    public ImapMessage createStatusMessage(ImapCommand command, String mailboxName, StatusDataItems statusDataItems, String tag) {
        return new StatusRequest(command, mailboxName, statusDataItems, tag);
    }

    public ImapMessage createStoreMessage(ImapCommand command, IdRange[] idSet, boolean silent, Boolean sign, Flags flags, boolean useUids, String tag) {
        return new StoreRequest(command, idSet, silent, flags, useUids, tag, sign);
    }

    public ImapMessage createSubscribeMessage(ImapCommand command, String mailboxName, String tag) {
        return new SubscribeRequest(command, mailboxName, tag);
    }

    public ImapMessage createUnsubscribeMessage(ImapCommand command, String mailboxName, String tag) {
        return new UnsubscribeRequest(command, mailboxName, tag);
    }

    public ImapMessage createBadRequestMessage(String message) {
        return new BadResponse(message);
    }

    public ImapMessage createCheckMessage(ImapCommand command, String tag) {
        return new CheckRequest(command, tag);
    }  
    
    
}
