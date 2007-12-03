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

package org.apache.james.mailboxmanager.mailbox;

import java.util.Collection;

import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.MailboxManagerException;

public interface Mailbox {

    /**
     * Example #mail.paul.lists.apache.james-dev (3rd level folder of user paul)
     * 
     * @return Full folder name with namespace
     * @throws MailboxManagerException
     */

    String getName() throws MailboxManagerException;

    int getMessageCount() throws MailboxManagerException;

    /** @return the key */
    String store(MimeMessage message) throws MailboxManagerException;

    /** @return keys */
    Collection list() throws MailboxManagerException;

    MimeMessage retrieve(String key) throws MailboxManagerException;

    /**
     * key changes by updating
     * 
     * @param key
     *            the current key
     * @return the new key
     */
    String update(String key, MimeMessage message)
            throws MailboxManagerException;

    void remove(String key) throws MailboxManagerException;

    boolean isWriteable();
}
