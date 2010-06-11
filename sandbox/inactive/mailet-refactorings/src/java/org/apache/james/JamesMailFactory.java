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
package org.apache.james;

import java.util.Collection;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailFactory;


/**

 */


public class JamesMailFactory implements MailFactory {

    /**
     * @throws MessagingException 
     * @see org.apache.mailet.MailFactory#newMail(java.lang.String, org.apache.mailet.MailAddress, java.util.Collection, javax.mail.internet.MimeMessage)
     */
    public Mail newMail(String id, MailAddress sender, Collection recipients, MimeMessage message) throws MessagingException {

       
        return new MailImpl(id, sender, recipients, message);
    }

    /**
     * @throws MessagingException 
     * @see org.apache.mailet.MailFactory#newMail(org.apache.mailet.Mail)
     */
    public Mail newMail(Mail originalMail) throws MessagingException {

        
        return  new MailImpl(originalMail);
    }

    /**
     * @see org.apache.mailet.MailFactory#newMail()
     */
    public Mail newMail() throws MessagingException {

        
        return new MailImpl();
    }
    
}


/* 
 *
 * PVCS Log History:
 * $Log$
 *
 */
