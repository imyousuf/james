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
package org.apache.james.experimental.imapserver.commands.imap4rev1;

import org.apache.james.experimental.imapserver.commands.ImapCommand;

public class StandardImap4Rev1CommandFactory implements Imap4Rev1CommandFactory {

    private final AppendCommand APPEND = new AppendCommand();   
    private final AuthenticateCommand AUTHENTICATE = new AuthenticateCommand();
    private final CapabilityCommand CAPABILITY = new CapabilityCommand();
    private final CheckCommand CHECK = new CheckCommand();
    private final CloseCommand CLOSE  = new CloseCommand();
    private final CopyCommand COPY = new CopyCommand();
    private final CreateCommand CREATE = new CreateCommand();
    private final DeleteCommand DELETE = new DeleteCommand();
    private final ExamineCommand EXAMINE = new ExamineCommand();
    private final ExpungeCommand EXPUNGE = new ExpungeCommand();
    private final FetchCommand FETCH = new FetchCommand();
    private final ListCommand LIST = new ListCommand();
    private final LoginCommand LOGIN = new LoginCommand();
    private final LogoutCommand LOGOUT = new LogoutCommand();
    private final LsubCommand LSUB = new LsubCommand();
    private final NoopCommand NOOP = new NoopCommand();
    private final RenameCommand RENAME = new RenameCommand();
    private final SearchCommand SEARCH = new SearchCommand();
    private final SelectCommand SELECT = new SelectCommand();
    private final StatusCommand STATUS = new StatusCommand();
    private final StoreCommand STORE = new StoreCommand();
    private final SubscribeCommand SUBSCRIBE = new SubscribeCommand();
    private final UidCommand UID = new UidCommand();
    private final UnsubscribeCommand UNSUBSCRIBE  = new UnsubscribeCommand();
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getAppend()
     */
    public ImapCommand getAppend() {
        return APPEND;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getAuthenticate()
     */
    public ImapCommand getAuthenticate() {
        return AUTHENTICATE;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getCapability()
     */
    public ImapCommand getCapability() {
        return CAPABILITY;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getCheck()
     */
    public ImapCommand getCheck() {
        return CHECK;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getClose()
     */
    public ImapCommand getClose() {
        return CLOSE;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getCopy()
     */
    public ImapCommand getCopy() {
        return COPY;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getCreate()
     */
    public ImapCommand getCreate() {
        return CREATE;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getDelete()
     */
    public ImapCommand getDelete() {
        return DELETE;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getExamine()
     */
    public ImapCommand getExamine() {
        return EXAMINE;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getExpunge()
     */
    public ImapCommand getExpunge() {
        return EXPUNGE;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getFetch()
     */
    public ImapCommand getFetch() {
        return FETCH;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getList()
     */
    public ImapCommand getList() {
        return LIST;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getLogin()
     */
    public ImapCommand getLogin() {
        return LOGIN;
    }

    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getLogout()
     */
    public ImapCommand getLogout() {
        return LOGOUT;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getLsub()
     */
    public ImapCommand getLsub() {
        return LSUB;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getNoop()
     */
    public ImapCommand getNoop() {
        return NOOP;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getRename()
     */
    public ImapCommand getRename() {
        return RENAME;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getSearch()
     */
    public ImapCommand getSearch() {
        return SEARCH;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getSelect()
     */
    public ImapCommand getSelect() {
        return SELECT;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getStatus()
     */
    public ImapCommand getStatus() {
        return STATUS;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getStore()
     */
    public ImapCommand getStore() {
        return STORE;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getSubscribe()
     */
    public ImapCommand getSubscribe() {
        return SUBSCRIBE;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getUid()
     */
    public ImapCommand getUid() {
        return UID;
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory#getUnsubscribe()
     */
    public ImapCommand getUnsubscribe() {
        return UNSUBSCRIBE;
    }
}
