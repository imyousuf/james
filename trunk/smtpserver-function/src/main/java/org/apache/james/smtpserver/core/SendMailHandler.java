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



package org.apache.james.smtpserver.core;

import java.util.Collection;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.MessageHook;
import org.apache.mailet.Mail;


/**
  * Adds the header to the message
  */
public class SendMailHandler extends AbstractLogEnabled implements MessageHook {

    private MailServer mailServer;
        
    /**
     * Gets the mail server.
     * @return the mailServer
     */
    public final MailServer getMailServer() {
        return mailServer;
    }

    /**
     * Sets the mail server.
     * @param mailServer the mailServer to set
     */
    @Resource(name="James")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    
    /**
     * Adds header to the message
     * @see org.apache.james.smtpserver#onMessage(SMTPSession)
     */
    public HookResult onMessage(SMTPSession session, Mail mail) {
        getLogger().debug("sending mail");

        try {
            mailServer.sendMail(mail);
            Collection theRecipients = mail.getRecipients();
            String recipientString = "";
            if (theRecipients != null) {
                recipientString = theRecipients.toString();
            }
            if (getLogger().isInfoEnabled()) {
                StringBuffer infoBuffer =
                     new StringBuffer(256)
                         .append("Successfully spooled mail ")
                         .append(mail.getName())
                         .append(" from ")
                         .append(mail.getSender())
                         .append(" on ")
                         .append(session.getRemoteIPAddress())
                         .append(" for ")
                         .append(recipientString);
                getLogger().info(infoBuffer.toString());
            }
        } catch (MessagingException me) {
            getLogger().error("Unknown error occurred while processing DATA.", me);
            return new HookResult(HookReturnCode.DENYSOFT,DSNStatus.getStatus(DSNStatus.TRANSIENT,DSNStatus.UNDEFINED_STATUS)+" Error processing message.");
        }
        return new HookResult(HookReturnCode.OK, DSNStatus.getStatus(DSNStatus.SUCCESS,DSNStatus.CONTENT_OTHER)+" Message received");
    }

}
