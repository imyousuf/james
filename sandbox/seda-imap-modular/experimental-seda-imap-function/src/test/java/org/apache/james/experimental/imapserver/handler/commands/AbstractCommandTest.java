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

package org.apache.james.experimental.imapserver.handler.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.james.api.imap.ImapSessionState;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.experimental.imapserver.ImapRequestHandler;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public abstract class AbstractCommandTest extends MockObjectTestCase
{

    ImapRequestHandler handler;
    Mock mockSession;
    Mock mockUsersRepository;
    Mock mockUser;
    Mock mockMailboxManager;

    public void setUp() {
        handler=new ImapRequestHandler();
        handler.enableLogging(new MockLogger());
        mockSession = mock ( ImapSession.class);
        mockUsersRepository = mock ( UsersRepository.class );
        mockUser = mock (User.class );
        mockMailboxManager = mock (MailboxManager.class);
    }
    
    public String handleRequest(String s) throws ProtocolException {
        ByteArrayInputStream is=new ByteArrayInputStream(s.getBytes());
        ByteArrayOutputStream os=new ByteArrayOutputStream();
        System.out.println("IN :"+s);
        handler.handleRequest(is,os,(ImapSession) mockSession.proxy());
        String out=os.toString();
        System.out.println("OUT:"+out);
        return out;
    }
    
    protected void setSessionState(ImapSessionState state) {
        mockSession.expects(atLeastOnce()).method("getState").will(returnValue(state));
    }
    
    protected void setUpMailboxManager() {
        mockSession.expects(atLeastOnce()).method("getMailboxManager").withNoArguments().will(returnValue(mockMailboxManager.proxy()));
    }
    

}
