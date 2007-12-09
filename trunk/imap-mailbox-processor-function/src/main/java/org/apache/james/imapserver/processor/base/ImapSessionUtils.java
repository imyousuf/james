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

package org.apache.james.imapserver.processor.base;

import java.util.Iterator;
import java.util.List;

import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.api.imap.process.ImapProcessor.Responder;
import org.apache.james.imap.message.response.base.AbstractImapResponse;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.services.User;

public class ImapSessionUtils {
    
    public static final String MAILBOX_USER_ATTRIBUTE_SESSION_KEY 
        = "org.apache.james.api.imap.MAILBOX_USER_ATTRIBUTE_SESSION_KEY";
    public static final String MAILBOX_MANAGER_ATTRIBUTE_SESSION_KEY 
        = "org.apache.james.api.imap.MAILBOX_MANAGER_ATTRIBUTE_SESSION_KEY";
    public static final String SELECTED_MAILBOX_ATTRIBUTE_SESSION_KEY 
        = "org.apache.james.api.imap.SELECTED_MAILBOX_ATTRIBUTE_SESSION_KEY";
    public static final String MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY 
    = "org.apache.james.api.imap.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY";
    
    public static void addUnsolicitedResponses(AbstractImapResponse response, ImapSession session, boolean useUids) {
        List unsolicitedResponses = session.unsolicitedResponses(useUids);
        response.addUnsolicitedResponses(unsolicitedResponses);
    }
    
    public static void addUnsolicitedResponses(ImapSession session, boolean useUids, final Responder responder) {
        final List unsolicitedResponses = session.unsolicitedResponses(useUids);
        for (Iterator it = unsolicitedResponses.iterator();it.hasNext();) {
            ImapResponseMessage message = (ImapResponseMessage) it.next();
            responder.respond(message);
        }
    }
    
    public static ImapMailbox getMailbox( final ImapSession session ) {
        ImapMailbox result 
            = (ImapMailbox) session.getAttribute(SELECTED_MAILBOX_ATTRIBUTE_SESSION_KEY);
        return result;
    }

    public static MailboxSession getMailboxSession( final ImapSession session) {
        final MailboxSession result = (MailboxSession) session.getAttribute( ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY );
        return result;
    }
    
    public static void setMailboxSession(final ImapSession session, final MailboxSession mailboxSession) {
        session.setAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY, mailboxSession);
    }
    
    public static User getUser( final ImapSession session ) {
        final User result = (User) session.getAttribute(MAILBOX_USER_ATTRIBUTE_SESSION_KEY);
        return result;
    }
    
    public static void setUser( final ImapSession session, final User user) {
        session.setAttribute(MAILBOX_USER_ATTRIBUTE_SESSION_KEY, user);
    }

}
