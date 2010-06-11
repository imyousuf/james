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

package org.apache.james.imapserver;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.mailboxmanager.torque.DefaultMailboxManager;
import org.apache.james.mailboxmanager.torque.DefaultUserManager;
import org.apache.james.services.FileSystem;
import org.apache.james.user.impl.file.FileUserMetaDataRepository;

public class DefaultImapFactory extends ImapFactory {

    
    public DefaultImapFactory(FileSystem fileSystem, UsersRepository users, Log logger) {
        super(fileSystem, users, new DefaultMailboxManager(new DefaultUserManager(
                new FileUserMetaDataRepository("var/users"), users), fileSystem, logger));
    }


    @Override
    public void configure( final HierarchicalConfiguration configuration ) throws ConfigurationException {
        super.configure(configuration);
        final MailboxManager mailbox = getMailbox();
        if (mailbox instanceof DefaultMailboxManager) {
            final DefaultMailboxManager manager = (DefaultMailboxManager) mailbox;
            manager.configure(configuration);
        }
    }

    @Override
    public void init() throws Exception {
        super.init();
        final MailboxManager mailbox = getMailbox();
        if (mailbox instanceof DefaultMailboxManager) {
            final DefaultMailboxManager manager = (DefaultMailboxManager) mailbox;
            manager.initialize();
        }
    }
    
  
}
