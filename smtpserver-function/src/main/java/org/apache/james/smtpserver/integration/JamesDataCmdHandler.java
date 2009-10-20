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
    
    /**
     * @param session
     * @param headerLineBuffer
     * @return
     * @throws MessagingException
     */
    /*
    private MailHeaders createNewReceivedMailHeaders(SMTPSession session) throws MessagingException {
        StringBuilder headerLineBuffer = new StringBuilder(512);
        MailHeaders newHeaders = new MailHeaders();
        
        String heloMode = (String) session.getConnectionState().get(SMTPSession.CURRENT_HELO_MODE);
        String heloName = (String) session.getConnectionState().get(SMTPSession.CURRENT_HELO_NAME);

        // Put our Received header first
        headerLineBuffer.append(RFC2822Headers.RECEIVED + ": from ")
                        .append(session.getRemoteHost());
        
        if (heloName != null) {
            headerLineBuffer.append(" (")
                            .append(heloMode)
                            .append(" ")
                            .append(heloName)
                            .append(") ");
        }
        
        headerLineBuffer.append(" ([")
                        .append(session.getRemoteIPAddress())
                        .append("])");

        newHeaders.addHeaderLine(headerLineBuffer.toString());
        headerLineBuffer.delete(0, headerLineBuffer.length());

        headerLineBuffer.append("          by ")
                        .append(session.getHelloName())
                        .append(" (")
                        .append(SOFTWARE_TYPE)
                        .append(") with ");
     
        // Check if EHLO was used 
        if ("EHLO".equals(heloMode)) {
            // Not successful auth
            if (session.getUser() == null) {
                headerLineBuffer.append("ESMTP");  
            } else {
                // See RFC3848
                // The new keyword "ESMTPA" indicates the use of ESMTP when the SMTP
                // AUTH [3] extension is also used and authentication is successfully
                // achieved.
                headerLineBuffer.append("ESMTPA");
            }
        } else {
            headerLineBuffer.append("SMTP");
        }
        
        headerLineBuffer.append(" ID ")
                        .append(session.getSessionID());

        if (((Collection) session.getState().get(SMTPSession.RCPT_LIST)).size() == 1) {
            // Only indicate a recipient if they're the only recipient
            // (prevents email address harvesting and large headers in
            //  bulk email)
            newHeaders.addHeaderLine(headerLineBuffer.toString());
            headerLineBuffer.delete(0, headerLineBuffer.length());
            headerLineBuffer.append("          for <")
                            .append(((List) session.getState().get(SMTPSession.RCPT_LIST)).get(0).toString())
                            .append(">;");
            newHeaders.addHeaderLine(headerLineBuffer.toString());
            headerLineBuffer.delete(0, headerLineBuffer.length());
        } else {
            // Put the ; on the end of the 'by' line
            headerLineBuffer.append(";");
            newHeaders.addHeaderLine(headerLineBuffer.toString());
            headerLineBuffer.delete(0, headerLineBuffer.length());
        }
        headerLineBuffer = null;
        newHeaders.addHeaderLine("          " + rfc822DateFormat.format(new Date()));
        return newHeaders;
    }
    */
}
