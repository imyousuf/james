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

package org.apache.james.imapserver.mock;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.TestConstants;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

public class MockMailServer implements MailServer, TestConstants
{

    public void sendMail(MailAddress sender, Collection recipients,
            MimeMessage msg) throws MessagingException
    {
        throw new RuntimeException("not implemented");

    }

    public void sendMail(MailAddress sender, Collection recipients,
            InputStream msg) throws MessagingException
    {
        throw new RuntimeException("not implemented");
    }

    public void sendMail(Mail mail) throws MessagingException
    {
        throw new RuntimeException("not implemented");
    }

    public void sendMail(MimeMessage message) throws MessagingException
    {
        throw new RuntimeException("not implemented");
    }

    Map userToMailRepo = new HashMap();

    public MailRepository getUserInbox(String userName) {
        return null;
    }

    public String getId()
    {
        throw new RuntimeException("not implemented");
    }

    public boolean addUser(String userName, String password)
    {
        throw new RuntimeException("not implemented");
    }

    public boolean isLocalServer(String serverName)
    {
        throw new RuntimeException("not implemented");
    }
    
    public boolean supportVirtualHosting() {
        return false;
    }

    public String getDefaultDomain() {
        return "localhost";
    }

    public String getHelloName() {
        return "localhost";
    }

}
