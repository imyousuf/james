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

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.Constants;
import org.apache.james.core.MailHeaders;
import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.ExtensibleHandler;
import org.apache.james.smtpserver.LineHandler;
import org.apache.james.smtpserver.MessageSizeException;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.SizeLimitedOutputStream;
import org.apache.james.smtpserver.WiringException;
import org.apache.james.smtpserver.hook.MessageHook;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.dates.RFC822DateFormat;

import javax.mail.MessagingException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;


/**
  * handles DATA command
 */
public class DataCmdHandler
    extends AbstractLogEnabled
    implements CommandHandler, ExtensibleHandler {

    private final class DataLineHandler implements LineHandler {
        public void onLine(SMTPSession session, byte[] line) {
            MimeMessageInputStreamSource mmiss = (MimeMessageInputStreamSource) session.getState().get(DATA_MIMEMESSAGE_STREAMSOURCE);
            OutputStream out = (OutputStream)  session.getState().get(DATA_MIMEMESSAGE_OUTPUTSTREAM);
            try {
                // 46 is "."
                // Stream terminated
                if (line.length == 3 && line[0] == 46) {
                    out.flush();
                    out.close();
                    
                    List recipientCollection = (List) session.getState().get(SMTPSession.RCPT_LIST);
                    MailImpl mail =
                        new MailImpl(session.getConfigurationData().getMailServer().getId(),
                                     (MailAddress) session.getState().get(SMTPSession.SENDER),
                                     recipientCollection);
                    MimeMessageCopyOnWriteProxy mimeMessageCopyOnWriteProxy = null;
                    try {
                        mimeMessageCopyOnWriteProxy = new MimeMessageCopyOnWriteProxy(mmiss);
                        mail.setMessage(mimeMessageCopyOnWriteProxy);
                        
                        mailPostProcessor(session, mail);
        
                        processExtensions(session, mail);
                        
                        session.popLineHandler();
                        
        
                    } catch (MessagingException e) {
                        // TODO probably return a temporary problem
                        getLogger().info("Unexpected error handling DATA stream",e);
                        session.writeSMTPResponse(new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unexpected error handling DATA stream."));
                    } finally {
                        ContainerUtil.dispose(mimeMessageCopyOnWriteProxy);
                        ContainerUtil.dispose(mmiss);
                        ContainerUtil.dispose(mail);
                    }
        
                    
                // DotStuffing.
                } else if (line[0] == 46 && line[1] == 46) {
                    out.write(line,1,line.length-1);
                // Standard write
                } else {
                    // TODO: maybe we should handle the Header/Body recognition here
                    // and if needed let a filter to cache the headers to apply some
                    // transormation before writing them to output.
                    out.write(line);
                }
                out.flush();
            } catch (IOException e) {
                SMTPResponse response;
                if (e != null && e instanceof MessageSizeException) {
                    // Add an item to the state to suppress
                    // logging of extra lines of data
                    // that are sent after the size limit has
                    // been hit.
                    session.getState().put(SMTPSession.MESG_FAILED, Boolean.TRUE);
                    // then let the client know that the size
                    // limit has been hit.
                    response = new SMTPResponse(SMTPRetCode.QUOTA_EXCEEDED,DSNStatus.getStatus(DSNStatus.PERMANENT,
                                    DSNStatus.SYSTEM_MSG_TOO_BIG) + " Error processing message: " + e.getMessage());
                  
                    StringBuffer errorBuffer = new StringBuffer(256).append(
                            "Rejected message from ").append(
                            session.getState().get(SMTPSession.SENDER).toString())
                            .append(" from host ").append(session.getRemoteHost())
                            .append(" (").append(session.getRemoteIPAddress())
                            .append(") exceeding system maximum message size of ")
                            .append(
                                    session.getConfigurationData()
                                            .getMaxMessageSize());
                    getLogger().error(errorBuffer.toString());
                } else {
                    response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR,DSNStatus.getStatus(DSNStatus.TRANSIENT,
                                    DSNStatus.UNDEFINED_STATUS) + " Error processing message: " + e.getMessage());
                    
                    getLogger().error(
                            "Unknown error occurred while processing DATA.", e);
                }
                session.popLineHandler();
                session.writeSMTPResponse(response);
                return;
            }
        }
    }

    private static final String DATA_MIMEMESSAGE_STREAMSOURCE = "org.apache.james.core.DataCmdHandler.DATA_MIMEMESSAGE_STREAMSOURCE";

    private static final String DATA_MIMEMESSAGE_OUTPUTSTREAM = "org.apache.james.core.DataCmdHandler.DATA_MIMEMESSAGE_OUTPUTSTREAM";

    private final static String SOFTWARE_TYPE = "JAMES SMTP Server "
                                                 + Constants.SOFTWARE_VERSION;

    /**
     * Static RFC822DateFormat used to generate date headers
     */
    private final static RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    // Keys used to store/lookup data in the internal state hash map

    private List messageHandlers;
    
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
        long maxMessageSize = session.getConfigurationData().getMaxMessageSize();
        if (maxMessageSize > 0) {
            if (getLogger().isDebugEnabled()) {
                StringBuffer logBuffer = new StringBuffer(128).append(
                        "Using SizeLimitedInputStream ").append(
                        " with max message size: ").append(maxMessageSize);
                getLogger().debug(logBuffer.toString());
            }
        }

        try {
            MimeMessageInputStreamSource mmiss = new MimeMessageInputStreamSource(session.getConfigurationData().getMailServer().getId());
            OutputStream out = mmiss.getWritableOutputStream();

            // Prepend output headers with out Received
            MailHeaders mh = createNewReceivedMailHeaders(session);
            for (Enumeration en = mh.getAllHeaderLines(); en.hasMoreElements(); ) {
                out.write(en.nextElement().toString().getBytes());
                out.write("\r\n".getBytes());
            }
            
            if (maxMessageSize > 0) {
                out = new SizeLimitedOutputStream(out, maxMessageSize);
            }

            session.getState().put(DATA_MIMEMESSAGE_STREAMSOURCE, mmiss);
            session.getState().put(DATA_MIMEMESSAGE_OUTPUTSTREAM, out);

            // out = new PipedOutputStream(messageIn);
            session.pushLineHandler(new DataLineHandler());
            
        } catch (IOException e1) {
            // TODO Define what to do.
            e1.printStackTrace();
        } catch (MessagingException e1) {
            e1.printStackTrace();
        }
        
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
                        .append(session.getConfigurationData().getHelloName())
                        .append(" (")
                        .append(SOFTWARE_TYPE)
                        .append(") with ");
     
        // Check if EHLO was used 
        if ("EHLO".equals(heloMode)) {
            headerLineBuffer.append("ESMTP");
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
     * @param session
     * @param mail
     */
    private void mailPostProcessor(SMTPSession session, MailImpl mail) {
    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("DATA");
        
        return implCommands;
    }


    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#getMarkerInterfaces()
     */
    public List getMarkerInterfaces() {
        List classes = new ArrayList(1);
        classes.add(MessageHook.class);
        return classes;
    }


    /**
     * @throws WiringException 
     * @see org.apache.james.smtpserver.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        if (MessageHook.class.equals(interfaceName)) {
            this.messageHandlers = extension;
            if (messageHandlers.size() == 0) {
                if (getLogger().isErrorEnabled()) {
                    getLogger()
                            .error(
                                    "No messageHandler configured. Check that SendMailHandler is configured in the SMTPHandlerChain");
                }
                throw new WiringException("No messageHandler configured");
            }
        }
    }

    /**
     * @param session
     */
    private void processExtensions(SMTPSession session, Mail mail) {
        if(mail != null && mail instanceof Mail && messageHandlers != null) {
            try {
                getLogger().debug("executing message handlers");
                int count = messageHandlers.size();
                for(int i =0; i < count; i++) {
                    SMTPResponse response = AbstractHookableCmdHandler.calcDefaultSMTPResponse(((MessageHook)messageHandlers.get(i)).onMessage(session, (Mail) mail));
                    
                    //if the response is received, stop processing of command handlers
                    if(response != null) {
                        session.writeSMTPResponse(response);
                        break;
                    }
                }
            } finally {
                // Dispose the mail object and remove it
                if(mail != null) {
                    ContainerUtil.dispose(mail);
                    mail = null;
                }
                //do the clean up
                session.resetState();
            }
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
