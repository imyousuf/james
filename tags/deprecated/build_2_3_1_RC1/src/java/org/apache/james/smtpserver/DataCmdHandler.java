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

package org.apache.james.smtpserver;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.Constants;
import org.apache.james.core.MailHeaders;
import org.apache.james.core.MailImpl;
import org.apache.james.fetchmail.ReaderInputStream;
import org.apache.james.util.CharTerminatedInputStream;
import org.apache.james.util.DotStuffingInputStream;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.james.util.watchdog.BytesReadResetInputStream;
import org.apache.mailet.MailAddress;
import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.dates.RFC822DateFormat;

import javax.mail.MessagingException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;


/**
  * handles DATA command
 */
public class DataCmdHandler
    extends AbstractLogEnabled
    implements CommandHandler {

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
     * The character array that indicates termination of an SMTP connection
     */
    private final static char[] SMTPTerminator = { '\r', '\n', '.', '\r', '\n' };

    /**
     * process DATA command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        doDATA(session, session.getCommandArgument());
    }


    /**
     * Handler method called upon receipt of a DATA command.
     * Reads in message data, creates header, and delivers to
     * mail server service for delivery.
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doDATA(SMTPSession session, String argument) {
        String responseString = null;
        if ((argument != null) && (argument.length() > 0)) {
            responseString = "500 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Unexpected argument provided with DATA command";
            session.writeResponse(responseString);
        }
        if (!session.getState().containsKey(SMTPSession.SENDER)) {
            responseString = "503 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" No sender specified";
            session.writeResponse(responseString);
        } else if (!session.getState().containsKey(SMTPSession.RCPT_LIST)) {
            responseString = "503 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" No recipients specified";
            session.writeResponse(responseString);
        } else {
            responseString = "354 Ok Send data ending with <CRLF>.<CRLF>";
            session.writeResponse(responseString);
            InputStream msgIn = new CharTerminatedInputStream(session.getInputStream(), SMTPTerminator);
            try {
                msgIn = new BytesReadResetInputStream(msgIn,
                                                      session.getWatchdog(),
                                                      session.getConfigurationData().getResetLength());

                // if the message size limit has been set, we'll
                // wrap msgIn with a SizeLimitedInputStream
                long maxMessageSize = session.getConfigurationData().getMaxMessageSize();
                if (maxMessageSize > 0) {
                    if (getLogger().isDebugEnabled()) {
                        StringBuffer logBuffer =
                            new StringBuffer(128)
                                    .append("Using SizeLimitedInputStream ")
                                    .append(" with max message size: ")
                                    .append(maxMessageSize);
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
                    responseString = "552 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SYSTEM_MSG_TOO_BIG)+" Error processing message: "
                                + e.getMessage();
                    StringBuffer errorBuffer =
                        new StringBuffer(256)
                            .append("Rejected message from ")
                            .append(session.getState().get(SMTPSession.SENDER).toString())
                            .append(" from host ")
                            .append(session.getRemoteHost())
                            .append(" (")
                            .append(session.getRemoteIPAddress())
                            .append(") exceeding system maximum message size of ")
                            .append(session.getConfigurationData().getMaxMessageSize());
                    getLogger().error(errorBuffer.toString());
                } else {
                    responseString = "451 "+DSNStatus.getStatus(DSNStatus.TRANSIENT,DSNStatus.UNDEFINED_STATUS)+" Error processing message: "
                                + me.getMessage();
                    getLogger().error("Unknown error occurred while processing DATA.", me);
                }
                session.writeResponse(responseString);
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
        // Put our Received header first
        headerLineBuffer.append(RFC2822Headers.RECEIVED + ": from ")
                        .append(session.getRemoteHost())
                        .append(" ([")
                        .append(session.getRemoteIPAddress())
                        .append("])");

        newHeaders.addHeaderLine(headerLineBuffer.toString());
        headerLineBuffer.delete(0, headerLineBuffer.length());

        headerLineBuffer.append("          by ")
                        .append(session.getConfigurationData().getHelloName())
                        .append(" (")
                        .append(SOFTWARE_TYPE)
                        .append(") with SMTP ID ")
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

}
