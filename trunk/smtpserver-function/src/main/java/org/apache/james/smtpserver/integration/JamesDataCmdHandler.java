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
package org.apache.james.smtpserver.integration;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.core.DataCmdHandler;


/**
  * handles DATA command
 */
public class JamesDataCmdHandler extends DataCmdHandler {

    
    static final String DATA_MIMEMESSAGE_STREAMSOURCE = "org.apache.james.core.DataCmdHandler.DATA_MIMEMESSAGE_STREAMSOURCE";

    static final String DATA_MIMEMESSAGE_OUTPUTSTREAM = "org.apache.james.core.DataCmdHandler.DATA_MIMEMESSAGE_OUTPUTSTREAM";

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
     * Handler method called upon receipt of a DATA command.
     * Reads in message data, creates header, and delivers to
     * mail server service for delivery.
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    protected SMTPResponse doDATA(SMTPSession session, String argument) {
        try {
            MimeMessageInputStreamSource mmiss = new MimeMessageInputStreamSource(mailServer.getId());
            OutputStream out = mmiss.getWritableOutputStream();
            /*
            // Prepend output headers with out Received
            MailHeaders mh = createNewReceivedMailHeaders(session);
            for (Enumeration en = mh.getAllHeaderLines(); en.hasMoreElements(); ) {
                out.write(en.nextElement().toString().getBytes());
                out.write("\r\n".getBytes());
            }
            */
            session.getState().put(DATA_MIMEMESSAGE_STREAMSOURCE, mmiss);
            session.getState().put(DATA_MIMEMESSAGE_OUTPUTSTREAM, out);

        } catch (IOException e) {
            session.getLogger().warn("Error creating temporary outputstream for incoming data",e);
            return new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unexpected error preparing to receive DATA.");
        } catch (MessagingException e) {
            session.getLogger().warn("Error creating mimemessagesource for incoming data",e);
            return new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unexpected error preparing to receive DATA.");
        }
        
        // out = new PipedOutputStream(messageIn);
        session.pushLineHandler(getLineHandler());
        
        return new SMTPResponse(SMTPRetCode.DATA_READY, "Ok Send data ending with <CRLF>.<CRLF>");
    }
    
}
