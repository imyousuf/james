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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.client.LoginCommand;
import org.apache.james.imapserver.util.MessageGenerator;
import org.apache.james.imapserver.util.UnsolicitedResponseGenerator;
import org.apache.james.mailboxmanager.MailboxManagerException;


public class SessionTest extends AbstractSessionTest
{

    String[] existing = {USER_MAILBOX_ROOT+".INBOX",USER_MAILBOX_ROOT+".test",USER_MAILBOX_ROOT+".test1",USER_MAILBOX_ROOT+".test1.test1a",USER_MAILBOX_ROOT+".test1.test1b",USER_MAILBOX_ROOT+".test2",USER_MAILBOX_ROOT+".test2.test2a",USER_MAILBOX_ROOT+".test2.test2b"};
    Set existingSet = null;

    public void setUp() throws Exception
    {
        super.setUp();
        existingSet=new HashSet(Arrays.asList(existing));
        createFolders(existing);
    }



    
    public void testLogin() throws ProtocolException, IOException, MessagingException
    {
        verifyCommand(new LoginCommand(USER_NAME,USER_PASSWORD));
    }
    
    public void testCreate() throws ProtocolException, MessagingException, IOException, MailboxManagerException{
        testLogin();
        assertFalse(folderExists(USER_MAILBOX_ROOT+".Trash"));
        String response = handleRequest((++counter)+" create \"Trash\"\n");
        assertEquals(counter+" OK CREATE completed.\r\n",response);
        assertTrue(folderExists(USER_MAILBOX_ROOT+".Trash"));
    }   
    
    public void testListInbox() throws ProtocolException, MessagingException, IOException {
        testLogin();
        String response = handleRequest((++counter)+" LIST \"\" \"INBOX\"\n");
        assertEquals("* LIST () \".\" INBOX\r\n"+counter+" OK LIST completed.\r\n",response);
    }
    
    public void testListInboxFullName() throws ProtocolException, MessagingException, IOException {
        testLogin();
        String response = handleRequest((++counter)+" LIST \"\" \""+USER_MAILBOX_ROOT+".INBOX\"\n");
        assertEquals("* LIST () \".\" "+USER_MAILBOX_ROOT+".INBOX\r\n"+counter+" OK LIST completed.\r\n",response);
    }
    
    public void testLsubAll() throws ProtocolException, MessagingException, IOException {
        testLogin();
        BufferedReader br = handleRequestReader((++counter)+" LSUB \""+USER_MAILBOX_ROOT+"\" \"*\"\n");
        String response=null;
        String start="* LSUB () \".\" ";
        while ((response=br.readLine())!=null) {
            //System.out.println("Parsing "+response);
            if (response.charAt(0)=='*') {
                assertTrue(response.startsWith(start));
                String name=response.substring(start.length());
                assertTrue(existingSet.remove(name));
 
            } else {
                break; 
            }
        }
        assertEquals(0,existingSet.size());
        assertEquals(counter+" OK LSUB completed.",response);
        assertNull(br.readLine());
        
        
    }
    public void testListAll() throws ProtocolException, MessagingException, IOException {
        testLogin();
        BufferedReader br = handleRequestReader((++counter)+" LIST \""+USER_MAILBOX_ROOT+"\" \"*\"\n");
        String response=null;
        String start="* LIST () \".\" ";
        while ((response=br.readLine())!=null) {
            System.out.println("Parsing "+response);
            if (response.charAt(0)=='*') {
                assertTrue(response.startsWith(start));
                String name=response.substring(start.length());
                assertTrue(existingSet.remove(name));

            } else {
                break; 
            }
        }
        assertEquals(0,existingSet.size());
        assertEquals(counter+" OK LIST completed.",response);
        assertNull(br.readLine());
    }
    
    
    public void testSelectNonEmpty() throws ProtocolException, MessagingException, IOException, MailboxManagerException {
        long uidv=getUidValidity(USER_MAILBOX_ROOT+".test");
        MimeMessage[] msgs=MessageGenerator.generateSimpleMessages(5);
        msgs[0].setFlag(Flag.SEEN,true);
        msgs[1].setFlag(Flag.SEEN,true);
        addUIDMessagesOpen(USER_MAILBOX_ROOT+".test",msgs);
        msgs=MessageGenerator.generateSimpleMessages(5);
        appendMessagesClosed(USER_MAILBOX_ROOT+".test",msgs);
        testLogin();
        String command=" SELECT \"test\"";

        UnsolicitedResponseGenerator rg=new UnsolicitedResponseGenerator();
        rg.addExists(10);
        rg.addRecent(5);
        rg.addFlags();
        rg.addUidValidity(uidv);
        rg.addFirstUnseen(3);
        rg.addPermanentFlags();
        
        String statusResponse="OK [READ-WRITE] SELECT completed.";
    
        verify(command,rg.getResponseSet(),statusResponse);
    }
    


    public void testAppendToSelectedInbox() throws ProtocolException, MessagingException, IOException, MailboxManagerException {
        testLogin();
        
        long uidv=getUidValidity(USER_MAILBOX_ROOT+".test");
        
        String command=" SELECT \"test\"";
        
        UnsolicitedResponseGenerator rg=new UnsolicitedResponseGenerator();
        rg.addExists(0);
        rg.addRecent(0);
        rg.addFlags();
        rg.addUidValidity(uidv);
        rg.addFirstUnseen(0);
        rg.addPermanentFlags();
        
        String statusResponse="OK [READ-WRITE] SELECT completed.";
    
        verify(command,rg.getResponseSet(),statusResponse);
        
        
        
        MimeMessage mm=MessageGenerator.generateSimpleMessage();
        String content=MessageGenerator.messageContentToString(mm);
        String request=" append \"test\" (\\Seen) {"+content.length()+"+}\n";
        request += content;
        
        rg=new UnsolicitedResponseGenerator();
        rg.addRecent(0);
        rg.addExists(1);

        verify(request,rg.getResponseSet(),"OK APPEND completed.");

        Message[] msgs=getMessages(USER_MAILBOX_ROOT+".test");
        assertEquals(1,msgs.length);
        assertEquals(content,MessageGenerator.messageContentToString(msgs[0]));
    }
}


