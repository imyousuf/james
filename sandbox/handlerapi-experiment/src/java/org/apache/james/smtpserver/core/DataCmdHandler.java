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

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.Constants;
import org.apache.james.core.MailHeaders;
import org.apache.james.core.MailImpl;
import org.apache.james.fetchmail.ReaderInputStream;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.ExtensibleHandler;
import org.apache.james.smtpserver.LineHandler;
import org.apache.james.smtpserver.MessageHandler;
import org.apache.james.smtpserver.MessageSizeException;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.SizeLimitedInputStream;
import org.apache.james.util.CharTerminatedInputStream;
import org.apache.james.util.DotStuffingInputStream;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.MailAddress;
import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.dates.RFC822DateFormat;

import javax.mail.MessagingException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.SequenceInputStream;
import java.io.StringReader;
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

    private final static String SOFTWARE_TYPE = "JAMES SMTP Server "
                                                 + Constants.SOFTWARE_VERSION;

    /**
     * Static RFC822DateFormat used to generate date headers
     */
    private final static RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    // Keys used to store/lookup data in the internal state hash map

    /**
     * The mail attribute holding the SMTP AUTH user name, if any.
     */
    private final static String SMTP_AUTH_USER_ATTRIBUTE_NAME = "org.apache.james.SMTPAuthUser";

    /**
     * The mail attribute which get set if the client is allowed to relay
     */
    private final static String SMTP_AUTH_NETWORK_NAME = "org.apache.james.SMTPIsAuthNetwork";

    /**
     * The character array that indicates termination of an SMTP connection
     */
    private final static char[] SMTPTerminator = { '\r', '\n', '.', '\r', '\n' };

    private List messageHandlers;

    /**
     * process DATA command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public SMTPResponse onCommand(SMTPSession session, String command, String parameters) {
        return doDATA(session, parameters);
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
        PipedInputStream messageIn = new PipedInputStream();
        Thread t = new Thread() {
            private PipedInputStream in;
            private SMTPSession session;

            public void run() {
                handleStream(session, in);
            }

            public Thread setParam(SMTPSession session, PipedInputStream in) {
                this.in = in;
                this.session = session;
                return this;
            }
        }.setParam(session, messageIn);
        
        t.start();
        
        OutputStream out;
        try {
            out = new PipedOutputStream(messageIn);
            session.pushLineHandler(new LineHandler() {

                private OutputStream out;
                private Thread worker;

                public void onLine(SMTPSession session, byte[] line) {
                    try {
                        out.write(line);
                        out.flush();
                        // 46 is "."
                        if (line.length == 3 && line[0] == 46) {
                            try {
                                worker.join();
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        
                        // Handle MessageHandlers
                        processExtensions(session);
                        
                    } catch (IOException e) {
                        // TODO Define what we have to do here!
                        e.printStackTrace();
                    }
                }

                public LineHandler setParam(OutputStream out, Thread t) {
                    this.out = out;
                    this.worker = t;
                    return this;
                };
            
            }.setParam(out,t));
        } catch (IOException e1) {
            // TODO Define what to do.
            e1.printStackTrace();
        }
        
        return new SMTPResponse("354", "Ok Send data ending with <CRLF>.<CRLF>");
    }
    

    public void handleStream(SMTPSession session, InputStream stream) {
        SMTPResponse response = null;
        InputStream msgIn = new CharTerminatedInputStream(stream, SMTPTerminator);
        try {
            // 2006/12/24 - We can remove this now that every single line is pushed and
            // reset the watchdog already in the handler.
            // This means we don't use resetLength anymore and we can remove
            // watchdog from the SMTPSession interface
            // msgIn = new BytesReadResetInputStream(msgIn, session.getWatchdog(),
            //         session.getConfigurationData().getResetLength());

            // if the message size limit has been set, we'll
            // wrap msgIn with a SizeLimitedInputStream
            long maxMessageSize = session.getConfigurationData()
                    .getMaxMessageSize();
            if (maxMessageSize > 0) {
                if (getLogger().isDebugEnabled()) {
                    StringBuffer logBuffer = new StringBuffer(128).append(
                            "Using SizeLimitedInputStream ").append(
                            " with max message size: ").append(maxMessageSize);
                    getLogger().debug(logBuffer.toString());
                }
                msgIn = new SizeLimitedInputStream(msgIn, maxMessageSize);
            }
            // Removes the dot stuffing
            msgIn = new DotStuffingInputStream(msgIn);
            // Parse out the message headers
            MailHeaders headers = new MailHeaders(msgIn);
            headers = processMailHeaders(session, headers);
            processMail(session, headers, msgIn);
            headers = null;
        } catch (MessagingException me) {
            // Grab any exception attached to this one.
            Exception e = me.getNextException();
            // If there was an attached exception, and it's a
            // MessageSizeException
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
                                DSNStatus.UNDEFINED_STATUS) + " Error processing message: " + me.getMessage());
                
                getLogger().error(
                        "Unknown error occurred while processing DATA.", me);
            }
            session.popLineHandler();
            session.writeSMTPResponse(response);
            return;
        } finally {
            if (msgIn != null) {
                try {
                    msgIn.close();
                } catch (Exception e) {
                    // Ignore close exception
                }
                msgIn = null;
            }
        }

    }







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
        StringBuffer headerLineBuffer = new StringBuffer(512);
        // We will rebuild the header object to put our Received header at the top
        Enumeration headerLines = headers.getAllHeaderLines();
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

        // Add all the original message headers back in next
        while (headerLines.hasMoreElements()) {
            newHeaders.addHeaderLine((String) headerLines.nextElement());
        }
        return newHeaders;
    }

    /**
     * Processes the mail message coming in off the wire.  Reads the
     * content and delivers to the spool.
     *
     * @param session SMTP session object
     * @param headers the headers of the mail being read
     * @param msgIn the stream containing the message content
     */
    private void processMail(SMTPSession session, MailHeaders headers, InputStream msgIn)
        throws MessagingException {
        ByteArrayInputStream headersIn = null;
        MailImpl mail = null;
        List recipientCollection = null;
        try {
            headersIn = new ByteArrayInputStream(headers.toByteArray());
            recipientCollection = (List) session.getState().get(SMTPSession.RCPT_LIST);
            mail =
                new MailImpl(session.getConfigurationData().getMailServer().getId(),
                             (MailAddress) session.getState().get(SMTPSession.SENDER),
                             recipientCollection,
                             new SequenceInputStream(new SequenceInputStream(headersIn, msgIn),
                                     new ReaderInputStream(new StringReader("\r\n"))));
            // Call mail.getSize() to force the message to be
            // loaded. Need to do this to enforce the size limit
            if (session.getConfigurationData().getMaxMessageSize() > 0) {
                mail.getMessageSize();
            }
            mail.setRemoteHost(session.getRemoteHost());
            mail.setRemoteAddr(session.getRemoteIPAddress());
            if (session.getUser() != null) {
                mail.setAttribute(SMTP_AUTH_USER_ATTRIBUTE_NAME, session.getUser());
            }
            
            if (session.isRelayingAllowed()) {
                mail.setAttribute(SMTP_AUTH_NETWORK_NAME,"true");
            }
            
            session.popLineHandler();
            
            session.setMail(mail);
        } catch (MessagingException me) {
            // if we get here, it means that we received a
            // MessagingException, which would happen BEFORE we call
            // session.setMail, so the mail object is still strictly
            // local to us, and we really should clean it up before
            // re-throwing the MessagingException for our call chain
            // to process.
            //
            // So why has this worked at all so far?  Initial
            // conjecture is that it has depended upon finalize to
            // call dispose.  Not in the MailImpl, which doesn't have
            // one, but even further down in the MimeMessageInputStreamSource.

            if (mail != null) {
                mail.dispose();
            }
            throw me;
        } finally {
            if (recipientCollection != null) {
                recipientCollection.clear();
            }
            recipientCollection = null;
            if (headersIn != null) {
                try {
                    headersIn.close();
                } catch (IOException ioe) {
                    // Ignore exception on close.
                }
            }
            headersIn = null;
        }

    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("DATA");
        
        return implCommands;
    }


    public Class getMarkerInterface() {
        return MessageHandler.class;
    }


    public void wireExtensions(List extension) {
        this.messageHandlers = extension;
    }

    /**
     * @param session
     */
    private void processExtensions(SMTPSession session) {
        if(session.getMail() != null && messageHandlers != null) {
            try {
                getLogger().debug("executing message handlers");
                int count = messageHandlers.size();
                for(int i =0; i < count; i++) {
                    SMTPResponse response = ((MessageHandler)messageHandlers.get(i)).onMessage(session);
                    
                    session.writeSMTPResponse(response);
                    
                    //if the response is received, stop processing of command handlers
                    if(response != null) {
                        break;
                    }
                }
            } finally {
                //do the clean up
                session.resetState();
            }
        }
    }


}
