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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.util.AbstractLogFactoryAware;

public abstract class AbstractGeneralMailbox extends AbstractLogFactoryAware implements GeneralMailbox {
    
    public Collection list() throws MailboxManagerException {
        MessageResult[] messageResult=getMessages(GeneralMessageSetImpl.all(), MessageResult.KEY);
        Collection result=new ArrayList(messageResult.length);
        for (int i = 0; i < messageResult.length; i++) {
            result.add(messageResult[i].getKey());
        }
        return result;
    }

    public void remove(String key) throws MailboxManagerException {
        remove(GeneralMessageSetImpl.oneKey(key));
    }

    public MimeMessage retrieve(String key) throws MailboxManagerException {
        MessageResult[] result = getMessages(GeneralMessageSetImpl.oneKey(key),
                MessageResult.MIME_MESSAGE);
        if (result != null && result.length == 1) {
            return result[0].getMimeMessage();
        } else {
            return null;
        }
    }

    public String store(MimeMessage message) throws MailboxManagerException {
        MessageResult result=appendMessage(message, new Date(), MessageResult.KEY);
        return result.getKey();
    }

    public String update(String key, MimeMessage message) throws MailboxManagerException {
        MessageResult result=updateMessage(GeneralMessageSetImpl.oneKey(key),message, MessageResult.KEY);
        return result.getKey();
    }
    
    

}
