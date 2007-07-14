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

package org.apache.james.imapserver.handler.session;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.client.FetchCommand;
import org.apache.james.imapserver.client.LoginCommand;
import org.apache.james.imapserver.client.SelectCommand;
import org.apache.james.imapserver.client.fetch.FetchBody;
import org.apache.james.imapserver.client.fetch.FetchHeader;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.util.MessageGenerator;
import org.apache.james.mailboxmanager.MailboxManagerException;

/**
 * 
 * 
 * 
 * TODO test one message number instead of message sets (1 instead of 1:1)
 *
 */

public class BodyFetchSessionTest extends AbstractSessionTest {
    
    String[] onlyInbox = {USER_MAILBOX_ROOT+".INBOX"};
    MimeMessage[] msgs= null;
    long[] uids = null;
    
    public void setUp() throws MailboxException, MessagingException, IOException, MailboxManagerException {
        super.setUp();
        msgs=MessageGenerator.generateSimpleMessages(4);
        createFolders(onlyInbox);
        // increase the uid
        appendMessagesClosed(USER_MAILBOX_ROOT+".INBOX",msgs);
        deleteAll(USER_MAILBOX_ROOT+".INBOX");
        uids=addUIDMessagesOpen(USER_MAILBOX_ROOT+".INBOX",msgs);
    }
    
    
    public void testFetchCompleteAndSize() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        
        FetchCommand fc=new FetchCommand(msgs,1,-1);
        fc.setFetchRfc822Size(true);
        fc.setFetchBody(new FetchBody(true));
        // TODO test \Seen get's not set because of peek and vice versa
        verifyCommandOrdered(fc);
    }
    public void testFetchCompleteHeaderAndSize() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        FetchCommand fc=new FetchCommand(msgs,1,-1);
        fc.setFetchRfc822Size(true);
        FetchBody fetchBody=new FetchBody(true);
        fetchBody.setFetchHeader(new FetchHeader());
        fc.setFetchBody(fetchBody);
        verifyCommandOrdered(fc);
    }
    public void testFetchSomeExistingHeaderAndSize() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        FetchCommand fc=new FetchCommand(msgs,1,-1);
        fc.setFetchRfc822Size(true);
        FetchHeader fetchHeader=new FetchHeader();
        fetchHeader.setFields(new String[] {"Date","From","To"});
        FetchBody fetchBody=new FetchBody(true);
        fetchBody.setFetchHeader(fetchHeader);
        fc.setFetchBody(fetchBody);
        verifyCommandOrdered(fc);
    }
    public void testFetchSomeNonExistingHeaderAndSize() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        FetchCommand fc=new FetchCommand(msgs,1,-1);
        fc.setFetchRfc822Size(true);
        FetchHeader fetchHeader=new FetchHeader();
        fetchHeader.setFields(new String[] {"Blob","Test","Oh"});
        FetchBody fetchBody=new FetchBody(true);
        fetchBody.setFetchHeader(fetchHeader);
        fc.setFetchBody(fetchBody);
        verifyCommandOrdered(fc);
    }
    public void testFetchSomeNoneAndExistingHeaderAndSize() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        FetchCommand fc=new FetchCommand(msgs,1,-1);
        fc.setFetchRfc822Size(true);
        FetchHeader fetchHeader=new FetchHeader();
        fetchHeader.setFields(new String[] {"To","Message-ID","Blob","Test","Oh"});
        FetchBody fetchBody=new FetchBody(true);
        fetchBody.setFetchHeader(fetchHeader);
        fc.setFetchBody(fetchBody);
        verifyCommandOrdered(fc);
    }

    
    public void testFetchSomeExistingHeader() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        
        FetchCommand fc=new FetchCommand(msgs,1,-1);
        FetchHeader fetchHeader=new FetchHeader();
        fetchHeader.setFields(new String[] {"Date","From","To"});
        FetchBody fetchBody=new FetchBody(true);
        fetchBody.setFetchHeader(fetchHeader);
        fc.setFetchBody(fetchBody);
        verifyCommandOrdered(fc);
    }        

    public void testFetchBodyNoParenthesisOneSeqNumber() throws ProtocolException, IOException, MessagingException, MailboxManagerException {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
        verifyCommand(new SelectCommand("INBOX", msgs, getUidValidity(USER_MAILBOX_ROOT+".INBOX")));
        msgs=getMessages(USER_MAILBOX_ROOT+".INBOX");
        
        FetchCommand fc=new FetchCommand(msgs,1);
        fc.setFetchBody(new FetchBody(true));
        fc.setUseParenthesis(false);
        verifyCommandOrdered(fc);
        
    }
}
