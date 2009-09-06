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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.Constants;
import org.apache.james.core.MailHeaders;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.ExtensibleHandler;
import org.apache.james.smtpserver.LineHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPRetCode;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.WiringException;
import org.apache.mailet.base.RFC2822Headers;
import org.apache.mailet.base.RFC822DateFormat;


/**
  * handles DATA command
 */
public class DataCmdHandler
    extends AbstractLogEnabled
    implements CommandHandler, ExtensibleHandler {

    public final class DataConsumerLineHandler implements LineHandler {
        /**
         * @see org.apache.james.smtpserver.LineHandler#onLine(org.apache.james.smtpserver.SMTPSession, byte[])
         */
        public void onLine(SMTPSession session, byte[] line) {
            // Discard everything until the end of DATA session
            if (line.length == 3 && line[0] == 46) {
                session.popLineHandler();
            }
        }
    }

    private final class DataLineFilterWrapper implements LineHandler {

        private DataLineFilter filter;
        private LineHandler next;
        
        public DataLineFilterWrapper(DataLineFilter filter, LineHandler next) {
            this.filter = filter;
            this.next = next;
        }
        public void onLine(SMTPSession session, byte[] line) {
            filter.onLine(session, line, next);
        }
                
    }
    
    static final String DATA_MIMEMESSAGE_STREAMSOURCE = "org.apache.james.core.DataCmdHandler.DATA_MIMEMESSAGE_STREAMSOURCE";

    static final String DATA_MIMEMESSAGE_OUTPUTSTREAM = "org.apache.james.core.DataCmdHandler.DATA_MIMEMESSAGE_OUTPUTSTREAM";

    private final static String SOFTWARE_TYPE = "JAMES SMTP Server "
                                                 + Constants.SOFTWARE_VERSION;

    /**
     * Static RFC822DateFormat used to generate date headers
     */
    private final static RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    // Keys used to store/lookup data in the internal state hash map

    private LineHandler lineHandler;

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
     * process DATA command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public SMTPResponse onCommand(SMTPSession session, String command, String parameters) {
        SMTPResponse response = doDATAFilter(session,parameters);
        
        if (response == null) {
            return doDATA(session, parameters);
        } else {
            return response;
        }
    }


    /**
     * Handler method called upon receipt of a DATA command.
     * Reads in message data, creates header, and delivers to
     * mail server service for delivery.
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private SMTPResponse doDATA(SMTPSession session, String argument) {
//        long maxMessageSize = session.getConfigurationData().getMaxMessageSize();
//        if (maxMessageSize > 0) {
//            if (getLogger().isDebugEnabled()) {
//                StringBuffer logBuffer = new StringBuffer(128).append(
//                        "Using SizeLimitedInputStream ").append(
//                        " with max message size: ").append(maxMessageSize);
//                getLogger().debug(logBuffer.toString());
//            }
//        }

        try {
            MimeMessageInputStreamSource mmiss = new MimeMessageInputStreamSource(mailServer.getId());
            OutputStream out = mmiss.getWritableOutputStream();

            // Prepend output headers with out Received
            MailHeaders mh = createNewReceivedMailHeaders(session);
            for (Enumeration en = mh.getAllHeaderLines(); en.hasMoreElements(); ) {
                out.write(en.nextElement().toString().getBytes());
                out.write("\r\n".getBytes());
            }
            
            session.getState().put(DATA_MIMEMESSAGE_STREAMSOURCE, mmiss);
            session.getState().put(DATA_MIMEMESSAGE_OUTPUTSTREAM, out);

        } catch (IOException e) {
            getLogger().warn("Error creating temporary outputstream for incoming data",e);
            return new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unexpected error preparing to receive DATA.");
        } catch (MessagingException e) {
            getLogger().warn("Error creating mimemessagesource for incoming data",e);
            return new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unexpected error preparing to receive DATA.");
        }
        
        // out = new PipedOutputStream(messageIn);
        session.pushLineHandler(lineHandler);
        
        return new SMTPResponse(SMTPRetCode.DATA_READY, "Ok Send data ending with <CRLF>.<CRLF>");
    }
    



    /**
     * TODO: the "createNewReceivedMailHeaders" part has been already ported.
     * The part that add a Date or a From if there is no From but a Sender has not been 
     * backported.
     */
    private MailHeaders processMailHeaders(SMTPSession session, MailHeaders headers)
        throws MessagingException {
        // If headers do not contains minimum REQUIRED headers fields,
        // add them
        if (!headers.isSet(RFC2822Headers.DATE)) {
            headers.setHeader(RFC2822Headers.DATE, rfc822DateFormat.format(new Date()));
        }
        if (!headers.isSet(RFC2822Headers.FROM) && session.getState().get(SMTPSession.SENDER) != null) {
            headers.setHeader(RFC2822Headers.FROM, session.getState().get(SMTPSession.SENDER).toString());
        }
        // RFC 2821 says that we cannot examine the message to see if
        // Return-Path headers are present.  If there is one, our
        // Received: header may precede it, but the Return-Path header
        // should be removed when making final delivery.
     // headers.removeHeader(RFC2822Headers.RETURN_PATH);
        // We will rebuild the header object to put our Received header at the top
        Enumeration headerLines = headers.getAllHeaderLines();
        MailHeaders newHeaders = createNewReceivedMailHeaders(session);

        // Add all the original message headers back in next
        while (headerLines.hasMoreElements()) {
            newHeaders.addHeaderLine((String) headerLines.nextElement());
        }
        return newHeaders;
    }


    /**
     * @param session
     * @param headerLineBuffer
     * @return
     * @throws MessagingException
     */
    private MailHeaders createNewReceivedMailHeaders(SMTPSession session) throws MessagingException {
        StringBuffer headerLineBuffer = new StringBuffer(512);
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
            // Not succesfull auth
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

    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        Collection<String> implCommands = new ArrayList<String>();
        implCommands.add("DATA");
        
        return implCommands;
    }


    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#getMarkerInterfaces()
     */
    public List getMarkerInterfaces() {
        List classes = new LinkedList();
        classes.add(DataLineFilter.class);
        return classes;
    }


    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        if (DataLineFilter.class.equals(interfaceName)) {
            LineHandler lineHandler = new DataConsumerLineHandler();
            for (int i = extension.size() - 1; i >= 0; i--) {
                lineHandler = new DataLineFilterWrapper((DataLineFilter) extension.get(i), lineHandler);
            }
            this.lineHandler = lineHandler;
        }
    }

    private SMTPResponse doDATAFilter(SMTPSession session, String argument) {
        if ((argument != null) && (argument.length() > 0)) {
            return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_COMMAND_UNRECOGNIZED, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Unexpected argument provided with DATA command");
        }
        if (!session.getState().containsKey(SMTPSession.SENDER)) {
            return new SMTPResponse(SMTPRetCode.BAD_SEQUENCE, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" No sender specified");
        } else if (!session.getState().containsKey(SMTPSession.RCPT_LIST)) {
            return new SMTPResponse(SMTPRetCode.BAD_SEQUENCE, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" No recipients specified");
        }
        return null;
    }

}
