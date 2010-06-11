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


import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.mailboxmanager.torque.DefaultMailboxManager;
import org.apache.james.mailboxmanager.torque.DefaultUserManager;
import org.apache.james.services.FileSystem;
import org.apache.james.user.impl.file.FileUserMetaDataRepository;

public class DefaultImapFactory extends ImapFactory {

    
    public DefaultImapFactory(FileSystem fileSystem, UsersRepository users, Logger logger) {
        super(fileSystem, users, logger, new DefaultMailboxManager(new DefaultUserManager(
                new FileUserMetaDataRepository("var/users"), users), fileSystem, logger));
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    @Override
    public void configure( final Configuration configuration ) throws ConfigurationException {
        super.configure(configuration);
        final MailboxManager mailbox = getMailbox();
        if (mailbox instanceof DefaultMailboxManager) {
            final DefaultMailboxManager manager = (DefaultMailboxManager) mailbox;
            manager.configure(configuration);
        }
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        final MailboxManager mailbox = getMailbox();
        if (mailbox instanceof DefaultMailboxManager) {
            final DefaultMailboxManager manager = (DefaultMailboxManager) mailbox;
            manager.initialize();
        }
    }
    
  
}
