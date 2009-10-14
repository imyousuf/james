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



package org.apache.james.smtpserver.protocol.core;

import javax.mail.internet.MimeMessage;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.james.smtpserver.protocol.hook.MessageHook;
import org.apache.mailet.Mail;


/**
  * Adds the header to the message
  */
public class SetMimeHeaderHandler implements MessageHook {

    /**
     * The header name and value that needs to be added
     */
    private String headerName;
    private String headerValue;


    /**
     * @see org.apache.james.socket.configuration.Configurable#configure(org.apache.commons.configuration.Configuration)
     */
    public void configure(Configuration handlerConfiguration) throws ConfigurationException {
       setHeaderName(handlerConfiguration.getString("headername"));
       setHeaderValue(handlerConfiguration.getString("headervalue"));
    }
    
    /**
     * Set the header name
     * 
     * @param headerName String which represent the header name
     */
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
    
    /**
     * Set the header value
     * 
     * @param headerValue String which represents the header value
     */
    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }


    /**
     * Adds header to the message
     *
     * @see org.apache.james.smtpserver.protocol.hook.MessageHook#onMessage(org.apache.james.smtpserver.protocol.SMTPSession, org.apache.mailet.Mail)
     */
    public HookResult onMessage(SMTPSession session, Mail mail) {
        try {
            MimeMessage message = mail.getMessage ();

            //Set the header name and value (supplied at init time).
            if(headerName != null) {
                message.setHeader(headerName, headerValue);
                message.saveChanges();
            }

        } catch (javax.mail.MessagingException me) {
            session.getLogger().error(me.getMessage());
        }
        
        return new HookResult(HookReturnCode.DECLINED);
    }



}
