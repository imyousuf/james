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

package org.apache.james.experimental.imapserver.mock;

import org.apache.james.experimental.imapserver.ImapHandlerConfigurationData;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;

public class MockImapHandlerConfigurationData implements
        ImapHandlerConfigurationData
{

    public MailServer mailServer;
    public UsersRepository usersRepository = new MockUsersRepository();
    public MailboxManagerProvider mailboxManagerProvider;

    public String getHelloName()
    {
        return "thats.my.host.org";
    }

    public int getResetLength()
    {
        return 24*1024;
    }

    public MailServer getMailServer()
    {
        if (mailServer==null) {
            mailServer=new MockMailServer();
        }
        return mailServer;
    }

    public UsersRepository getUsersRepository()
    {
        
        return usersRepository;
    }


    public MailboxManagerProvider getMailboxManagerProvider() {
        if (mailboxManagerProvider==null) {
            try {
                mailboxManagerProvider=MailboxManagerProviderSingleton.getMailboxManagerProviderInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return mailboxManagerProvider;
    }

}
