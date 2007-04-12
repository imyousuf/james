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
 * Creates messages.
 * Implementations may support message pooling but this is not required.
 */
public interface ImapMessageFactory {

    public ImapCommandMessage createErrorMessage(String message, String tag);
    
    public ImapCommandMessage createBadRequestMessage(final String message);
    
    public ImapCommandMessage createAppendMessage(ImapCommand command, String mailboxName, Flags flags, 
            Date datetime, MimeMessage message, String tag); 
    
    public ImapCommandMessage  createAuthenticateMessage(final ImapCommand command, 
            final String authType, final String tag);
        
    public ImapCommandMessage createCapabilityMessage(final ImapCommand command, final String tag);

    public ImapCommandMessage createCheckMessage(final ImapCommand command, final String tag);

    public ImapCommandMessage createNoopMessage(final ImapCommand command, final String tag);
    
    public ImapCommandMessage createCloseMessage(final ImapCommand command, final String tag);

    public ImapCommandMessage createCopyMessage(final ImapCommand command, final IdRange[] idSet, final String mailboxName, 
            final boolean useUids, final String tag);
    
    public ImapCommandMessage createCreateMessage(final ImapCommand command, final String mailboxName, final String tag);

    public ImapCommandMessage createDeleteMessage(final ImapCommand command, final String mailboxName, final String tag);
    
    public ImapCommandMessage createExamineMessage(final ImapCommand command, final String mailboxName, final String tag);

    public ImapCommandMessage createExpungeMessage(final ImapCommand command, final String tag);

    public ImapCommandMessage createFetchMessage(final ImapCommand command, final boolean useUids, final IdRange[] idSet, 
            final FetchRequest fetch, String tag);
    
    public ImapCommandMessage createListMessage(final ImapCommand command, final String referenceName, final String mailboxPattern,
            final String tag);
    
    public ImapCommandMessage createLoginMessage(final ImapCommand command, final String userid, final String password, String tag);

    public ImapCommandMessage createLogoutMessage(final ImapCommand command, final String tag);

    public ImapCommandMessage createLsubMessage(ImapCommand command, String referenceName, String mailboxPattern, String tag);

    public ImapCommandMessage createRenameMessage(final ImapCommand command, final String existingName, final String newName, 
            final String tag);
    
    public ImapCommandMessage createSearchImapMessage(final ImapCommand command, final SearchTerm searchTerm, final boolean useUids,
            final String tag);
    
    public ImapCommandMessage createSelectMessage(final ImapCommand command, final String mailboxName, final String tag);

    public ImapCommandMessage createStatusMessage(final ImapCommand command, final String mailboxName, final StatusDataItems statusDataItems, final String tag) ;

    public ImapCommandMessage createStoreMessage(final ImapCommand command, final IdRange[] idSet, final StoreDirective directive, final Flags flags, 
         final boolean useUids, final String tag); 
    
    public ImapCommandMessage createSubscribeMessage(final ImapCommand command, final String mailboxName, final String tag); 

    public ImapCommandMessage createUnsubscribeMessage(final ImapCommand command, final String mailboxName, final String tag);
}
