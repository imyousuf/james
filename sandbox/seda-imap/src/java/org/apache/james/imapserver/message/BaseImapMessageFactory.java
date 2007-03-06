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
package org.apache.james.imapserver.message;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.apache.james.imapserver.commands.ImapCommand;

/**
 * Naive, factory creates unpooled instances.
 */
public class BaseImapMessageFactory implements ImapMessageFactory {

    public ImapCommandMessage createErrorMessage(String message, String tag) {
        return new ErrorResponseMessage( message, tag );
    }

    public ImapCommandMessage createAppendMessage(ImapCommand command, String mailboxName, Flags flags, Date datetime, MimeMessage message, String tag) {
        return new AppendCommandMessage(command, mailboxName, 
                flags, datetime, message, tag);
    }

    public ImapCommandMessage createAuthenticateMessage(ImapCommand command, String authType, String tag) {
        return new AuthenticateCommandMessage(command, authType, tag);
    }

    public ImapCommandMessage createCapabilityMessage(ImapCommand command, String tag) {
        return new CapabilityCommandMessage(command, tag);
    }

    public ImapCommandMessage createCompleteMessage(ImapCommand command, boolean useUids, String tag) {
        return new CompleteCommandMessage(command, useUids, tag);
    }

    public ImapCommandMessage createCloseMessage(ImapCommand command, String tag) {
        return new CloseCommandMessage(command, tag);
    }

    public ImapCommandMessage createCopyMessage(ImapCommand command, IdRange[] idSet, String mailboxName, boolean useUids, String tag) {
        return new CopyCommandMessage(command, idSet, mailboxName, useUids, tag);
    }

    public ImapCommandMessage createCreateMessage(ImapCommand command, String mailboxName, String tag) {
        return new CreateCommandMessage(command, mailboxName, tag);
    }

    public ImapCommandMessage createDeleteMessage(ImapCommand command, String mailboxName, String tag) {
        return new DeleteCommandMessage( command, mailboxName, tag );
    }

    public ImapCommandMessage createExamineMessage(ImapCommand command, String mailboxName, String tag) {
        return new SelectCommandMessage(command, mailboxName, true, tag);
    }

    public ImapCommandMessage createExpungeMessage(ImapCommand command, String tag) {
        return new ExpungeCommandMessage(command, tag);
    }

    public ImapCommandMessage createFetchMessage(ImapCommand command, boolean useUids, IdRange[] idSet, FetchRequest fetch, String tag) {
        return new FetchCommandMessage(command, useUids, idSet, fetch, tag);
    }

    public ImapCommandMessage createListMessage(ImapCommand command, String referenceName, String mailboxPattern, String tag) {
        return new ListCommandMessage(command, referenceName, mailboxPattern, tag);
    }

    public ImapCommandMessage createLoginMessage(ImapCommand command, String userid, String password, String tag) {
        return new LoginCommandMessage(command, userid, password, tag);
    }

    public ImapCommandMessage createLogoutMessage(ImapCommand command, String tag) {
        return new LogoutCommandMessage(command, tag);
    }

    public ImapCommandMessage createLsubMessage(ImapCommand command, String referenceName, String mailboxPattern, String tag) {
        return new LsubListCommandMessage(command, referenceName, mailboxPattern, tag);
    }

    public ImapCommandMessage createRenameMessage(ImapCommand command, String existingName, String newName, String tag) {
        return new RenameCommandMessage(command, existingName, newName, tag);
    }

    public ImapCommandMessage createSearchImapMessage(ImapCommand command, SearchTerm searchTerm, boolean useUids, String tag) {
        return new SearchImapCommandMessage(command, searchTerm, useUids, tag);
    }

    public ImapCommandMessage createSelectMessage(ImapCommand command, String mailboxName, String tag) {
        return  new SelectCommandMessage(command, mailboxName, false, tag);
    }

    public ImapCommandMessage createStatusMessage(ImapCommand command, String mailboxName, StatusDataItems statusDataItems, String tag) {
        return new StatusCommandMessage(command, mailboxName, statusDataItems, tag);
    }

    public ImapCommandMessage createStoreMessage(ImapCommand command, IdRange[] idSet, StoreDirective directive, Flags flags, boolean useUids, String tag) {
        return new StoreCommandMessage(command, idSet, directive, flags, useUids, tag);
    }

    public ImapCommandMessage createSubscribeMessage(ImapCommand command, String mailboxName, String tag) {
        return new SubscribeCommandMessage(command, mailboxName, tag);
    }

    public ImapCommandMessage createUnsubscribeMessage(ImapCommand command, String mailboxName, String tag) {
        return new UnsubscribeCommandMessage(command, mailboxName, tag);
    }

    public ImapCommandMessage createBadRequestMessage(String message) {
        return new BadResponseMessage(message);
    }  
    
    
}
