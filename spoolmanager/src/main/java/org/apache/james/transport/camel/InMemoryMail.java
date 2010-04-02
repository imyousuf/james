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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageUtil;
import org.apache.mailet.Mail;

/**
 * This is just used for the JMS spooling atm. It is super inefficient because it store the whole MimeMessage in memory.
 * 
 * This needs to get fixed
 *
 */
public class InMemoryMail extends MailImpl {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private byte[] mimeMessage;
    
    public InMemoryMail(Mail mail) throws MessagingException {
        super(mail);
        setMessage(mail.getMessage());
        
    }
   
    @Override
    public MimeMessage getMessage() throws MessagingException {
        MimeMessage m = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(mimeMessage));
        return m;
    }

    @Override
    public long getMessageSize() throws MessagingException {
        return MimeMessageUtil.calculateMessageSize(getMessage());
    }

   
    @Override
    public void setMessage(MimeMessage arg0) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            arg0.writeTo(out);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     
        mimeMessage = out.toByteArray();
        
        // we have the MimeMessage copied to the byte array, so time to dispose the shared stuff
        super.dispose();
   
    }
    
    public void dispose() {
        // clear the byte array
        mimeMessage = null;
        
        super.dispose();
    }

}
