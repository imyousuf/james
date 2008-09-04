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
package org.apache.james.api.imap.imap4rev1;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.FetchData;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.StatusDataItems;
import org.apache.james.api.imap.message.request.SearchKey;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;

/**
 * Creates messages.
 * Implementations may support message pooling but this is not required.
 */
public interface Imap4Rev1MessageFactory {

    /**
     * Creates a tagged BAD status response.
     * @param tag <code>CharSequence</code>, not null
     * @param command <code>ImapCommand</code>, not null
     * @param displayTextKey key to the human readable code to be displayed
     * @return <code>StatusResponse</code>, not null
     */
    public StatusResponse taggedBad(String tag, ImapCommand command, HumanReadableTextKey displayTextKey);
    
    public StatusResponse bye(HumanReadableTextKey message);
    
    public ImapMessage createAppendMessage(ImapCommand command, String mailboxName, Flags flags, 
            Date datetime, MimeMessage message, String tag); 
    
    public ImapMessage  createAuthenticateMessage(final ImapCommand command, final String authType, final String tag);
        
    public ImapMessage createCapabilityMessage(final ImapCommand command, final String tag);

    public ImapMessage createCheckMessage(final ImapCommand command, final String tag);

    public ImapMessage createNoopMessage(final ImapCommand command, final String tag);
    
    public ImapMessage createCloseMessage(final ImapCommand command, final String tag);

    public ImapMessage createCopyMessage(final ImapCommand command, final IdRange[] idSet, final String mailboxName, 
            final boolean useUids, final String tag);
    
    public ImapMessage createCreateMessage(final ImapCommand command, final String mailboxName, final String tag);

    public ImapMessage createDeleteMessage(final ImapCommand command, final String mailboxName, final String tag);
    
    public ImapMessage createExamineMessage(final ImapCommand command, final String mailboxName, final String tag);

    public ImapMessage createExpungeMessage(final ImapCommand command, final String tag);

    public ImapMessage createFetchMessage(final ImapCommand command, final boolean useUids, final IdRange[] idSet, 
            final FetchData fetch, String tag);
    
    public ImapMessage createListMessage(final ImapCommand command, final String referenceName, final String mailboxPattern,
            final String tag);
    
    public ImapMessage createLoginMessage(final ImapCommand command, final String userid, final String password, String tag);

    public ImapMessage createLogoutMessage(final ImapCommand command, final String tag);

    public ImapMessage createLsubMessage(ImapCommand command, String referenceName, String mailboxPattern, String tag);

    public ImapMessage createRenameMessage(final ImapCommand command, final String existingName, final String newName, 
            final String tag);
    
    public ImapMessage createSearchMessage(final ImapCommand command, final SearchKey key, final boolean useUids,
            final String tag);
    
    public ImapMessage createSelectMessage(final ImapCommand command, final String mailboxName, final String tag);

    public ImapMessage createStatusMessage(final ImapCommand command, final String mailboxName, final StatusDataItems statusDataItems, final String tag) ;

    public ImapMessage createStoreMessage(final ImapCommand command, final IdRange[] idSet, boolean silent, Boolean sign, 
         final Flags flags, final boolean useUids, final String tag); 
    
    public ImapMessage createSubscribeMessage(final ImapCommand command, final String mailboxName, final String tag); 

    public ImapMessage createUnsubscribeMessage(final ImapCommand command, final String mailboxName, final String tag);
}
