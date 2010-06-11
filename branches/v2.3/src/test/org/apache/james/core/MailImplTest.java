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


package org.apache.james.core;

import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.test.mock.javaxmail.MockMimeMessage;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import java.util.ArrayList;

public class MailImplTest extends MailTestAllImplementations {

    protected Mail createMailImplementation() {
        return new MailImpl();
    }

    public void testConstr1() throws MessagingException {
        MailImpl mail = new MailImpl();

        helperTestInitialState(mail);
        helperTestMessageSize(mail, 0); // MimeMessageWrapper default is 0
        assertNull("no initial message", mail.getMessage());
        assertNull("no initial sender", mail.getSender());
        assertNull("no initial name", mail.getName());
    }

    public void testConstr2() throws MessagingException {
        ArrayList recepients = new ArrayList();
        String name = new MockMailServer().getId();
        String sender = "sender@localhost";
        MailAddress senderMailAddress = new MailAddress(sender);
        MailImpl mail = new MailImpl(name, senderMailAddress, recepients);

        helperTestInitialState(mail); // MimeMessageWrapper default is 0
        helperTestMessageSize(mail, 0); // MimeMessageWrapper default is 0
        assertNull("no initial message", mail.getMessage());
        assertEquals("sender", sender, mail.getSender().toString());
        assertEquals("name", name, mail.getName());

        mail.setMessage(new MockMimeMessage());
        assertNotNull("message", mail.getMessage());
    }

    public void testConstr3() throws MessagingException {
        ArrayList recepients = new ArrayList();
        String name = new MockMailServer().getId();
        String sender = "sender@localhost";
        MailAddress senderMailAddress = new MailAddress(sender);
        MockMimeMessage mimeMessage = new MockMimeMessage();
        MailImpl mail = new MailImpl(name, senderMailAddress, recepients, mimeMessage);

        helperTestInitialState(mail); 
        helperTestMessageSize(mail, 0);
        assertEquals("initial message", mimeMessage.getMessageID(), mail.getMessage().getMessageID());
        assertEquals("sender", sender, mail.getSender().toString());
        assertEquals("name", name, mail.getName());
        mail.dispose();
    }

    public void testDuplicate() throws MessagingException {
        MailImpl mail = new MailImpl();
        MailImpl duplicate = (MailImpl) mail.duplicate();
        assertNotSame("is real duplicate", mail, duplicate);
        helperTestInitialState(duplicate);
        helperTestMessageSize(duplicate, 0);
    }

    public void testDuplicateNewName() throws MessagingException {
        String newName = "aNewName";
        
        MailImpl mail = new MailImpl();
        assertFalse("before + after names differ", newName.equals(mail.getName()));
        
        MailImpl duplicate = (MailImpl) mail.duplicate(newName);
        assertEquals("new name set", newName, duplicate.getName());
        helperTestInitialState(duplicate);
        helperTestMessageSize(duplicate, 0);
    }
}
