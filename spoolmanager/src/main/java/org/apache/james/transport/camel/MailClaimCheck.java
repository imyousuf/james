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



import java.io.OutputStream;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.james.SpoolMessageStore;
import org.apache.james.lifecycle.LifecycleUtil;
import org.apache.mailet.Mail;

/**
 * Bean which is used in the camel route to store the real message content to some external
 * storage. This is needed because JMS is not really the best solution for big content
 * 
 * This Bean should get called before the Exchange object get send the JMS Endpoint
 *
 */
public final class MailClaimCheck {
    
    private SpoolMessageStore spoolMessageStore;

    @Resource(name="spoolMessageStore")
    public void setSpooolMessageStore(SpoolMessageStore spoolMessageStore) {
        this.spoolMessageStore = spoolMessageStore;
    }
    
    
    /**
     * Save the Email Message to an external storage and dispose it 
     * 
     * @param exchange
     * @param mail
     * @throws Exception
     */
    public void saveMessage(Exchange exchange, @Body Mail mail) throws Exception{
        
        MimeMessage m = mail.getMessage();

        // Check if the message is not null first.. This could be the case if the message was set to
        // null before or was disposed
        if (m != null) {
            OutputStream out = spoolMessageStore.saveMessage(mail.getName());
            // write the message to the store
            m.writeTo(out);
        
            // close stream
            out.close();
            
            // dispose the mail now, to make sure every stream is closed etc
            // Without that we will get a problem on windows later..
            LifecycleUtil.dispose(m);
            LifecycleUtil.dispose(mail);
            
        }
        
    }
}
