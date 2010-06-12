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
package org.apache.james.transport.camel;

import java.io.IOException;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.james.SpoolMessageStore;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.mailet.Mail;

/**
 * Enricher which will fetch the Email Message from an external storage and add it to the Mail
 * object which is stored in the {@link Body} of the {@link Exchange}. 
 * 
 * This should be used after the {@link Exchange} was consumed from the JMS queue
 * 
 *
 */
public final class MailEnricher {
    
    
    private SpoolMessageStore spoolMessageStore;

    @Resource(name="spoolMessageStore")
    public void setSpooolMessageStore(SpoolMessageStore spoolMessageStore) {
        this.spoolMessageStore = spoolMessageStore;
    }
    
    
    /**
     * Get the Email Message from external storage and add it to the Mail object in the {@link Body}
     * 
     * @param exchange
     * @param mail
     * @throws MessagingException
     */
    public void enrich(Exchange exchange, @Body Mail mail) throws MessagingException {
        String key = mail.getName();
        
        // put back the MimeMessage
        try {
            mail.setMessage(new MimeMessageCopyOnWriteProxy(spoolMessageStore.getMessage(key)));
        } catch (IOException e) {
            throw new MessagingException("Unable to enrich mail " + mail, e);
        }
    }

}
