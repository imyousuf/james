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
package org.apache.james.lmtpserver;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStream;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.lifecycle.api.LifecycleUtil;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.protocols.api.LineHandler;
import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPRetCode;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.core.DataLineFilter;
import org.apache.james.protocols.smtp.dsn.DSNStatus;
import org.apache.james.smtpserver.SMTPConstants;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * Handler which takes care of deliver the mail to the recipients INBOX
 * 
 *
 */
public class DataLineLMTPMessageHookHandler implements DataLineFilter {
    private UsersRepository users;
    private MailboxManager mailboxManager;

   @Resource(name="usersrepository")
   public final void setUsersRepository(UsersRepository users) {
       this.users = users;
   }
   
   @Resource(name="mailboxmanager")
   public final void setMailboxManager(MailboxManager mailboxManager) {
       this.mailboxManager = mailboxManager;
   }
   
    @SuppressWarnings("unchecked")
    public void onLine(SMTPSession session, byte[] line, LineHandler<SMTPSession> next) {
        MimeMessageInputStreamSource mmiss = (MimeMessageInputStreamSource) session.getState().get(SMTPConstants.DATA_MIMEMESSAGE_STREAMSOURCE);

        try {
            OutputStream out = mmiss.getWritableOutputStream();

            // 46 is "."
            // Stream terminated
            if (line.length == 3 && line[0] == 46) {
                out.flush();
                out.close();
                
                List recipientCollection = (List) session.getState().get(SMTPSession.RCPT_LIST);
                MailImpl mail =
                    new MailImpl(MailImpl.getId(),
                                 (MailAddress) session.getState().get(SMTPSession.SENDER),
                                 recipientCollection);
                
                // store mail in the session so we can be sure it get disposed later
                session.getState().put(SMTPConstants.MAIL, mail);
                
                MimeMessageCopyOnWriteProxy mimeMessageCopyOnWriteProxy = null;
                try {
                    mimeMessageCopyOnWriteProxy = new MimeMessageCopyOnWriteProxy(mmiss);
                    mail.setMessage(mimeMessageCopyOnWriteProxy);
                    
                    deliverMail(session, mail);
                    
                    session.popLineHandler();
                           
                    // do the clean up
                    session.resetState();
                    
                } catch (MessagingException e) {
                    // TODO probably return a temporary problem
                    session.getLogger().info("Unexpected error handling DATA stream",e);
                    session.writeResponse(new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unexpected error handling DATA stream."));
                } finally {
                    LifecycleUtil.dispose(mimeMessageCopyOnWriteProxy);
                    LifecycleUtil.dispose(mmiss);
                    LifecycleUtil.dispose(mail);
                }
    
                
            // DotStuffing.
            } else if (line[0] == 46 && line[1] == 46) {
                out.write(line,1,line.length-1);
            // Standard write
            } else {
                // TODO: maybe we should handle the Header/Body recognition here
                // and if needed let a filter to cache the headers to apply some
                // transformation before writing them to output.
                out.write(line);
            }
            out.flush();
        } catch (IOException e) {
            LifecycleUtil.dispose(mmiss);

            SMTPResponse response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR,DSNStatus.getStatus(DSNStatus.TRANSIENT,
                            DSNStatus.UNDEFINED_STATUS) + " Error processing message: " + e.getMessage());
            
            session.getLogger().error(
                    "Unknown error occurred while processing DATA.", e);
            session.writeResponse(response);
            return;
        }  
    }

    /**
     * Deliver mail to mailboxes
     * 
     * @param session
     * @param mail
     */
    @SuppressWarnings("unchecked")
    protected void deliverMail(SMTPSession session, Mail mail) {
        Iterator<MailAddress> recipients = mail.getRecipients().iterator();
        while (recipients.hasNext()) {
            MailAddress recipient = recipients.next();
            String username;
            SMTPResponse response = null;

            try {

                if (users.supportVirtualHosting()) {
                    username = recipient.toString();
                } else {
                    username = recipient.getLocalPart();
                }

                MailboxSession mailboxSession = mailboxManager.createSystemSession(username, session.getLogger());
                MailboxPath inbox = MailboxPath.inbox(username);

                mailboxManager.startProcessingRequest(mailboxSession);

                // create inbox if not exist
                if (mailboxManager.mailboxExists(inbox, mailboxSession) == false) {
                    mailboxManager.createMailbox(inbox, mailboxSession);
                }
                mailboxManager.getMailbox(MailboxPath.inbox(username), mailboxSession).appendMessage(new MimeMessageInputStream(mail.getMessage()), new Date(), mailboxSession, true, null);
                mailboxManager.endProcessingRequest(mailboxSession);
                response = new SMTPResponse(SMTPRetCode.MAIL_OK, DSNStatus.getStatus(DSNStatus.SUCCESS, DSNStatus.CONTENT_OTHER) + " Message received");

            } catch (MessagingException e) {
                session.getLogger().info("Unexpected error handling DATA stream", e);
                response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.UNDEFINED_STATUS) + " Temporary error deliver message to " + recipient);
            } catch (MailboxException e) {
                session.getLogger().info("Unexpected error handling DATA stream", e);
                response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.UNDEFINED_STATUS) + " Temporary error deliver message to " + recipient);
            } catch (UsersRepositoryException e) {
                session.getLogger().info("Unexpected error handling DATA stream", e);
                response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.UNDEFINED_STATUS) + " Temporary error deliver message to " + recipient);
            }
            session.writeResponse(response);
        }
        
        
    }
   

}
