/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.transport.mailets;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;

import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.james.util.RFC2822Headers;
import org.apache.james.util.RFC822DateFormat;
import org.apache.james.Constants;
import org.apache.james.util.mail.MimeMultipartReport;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.MatchResult;




/**
 *
 * <P>Generates a Delivery Status Notification (DSN)
 * Note that this is different than a mail-client's
 * reply, which would use the Reply-To or From header.</P>
 * <P>Bounced messages are attached in their entirety (headers and
 * content) and the resulting MIME part type is "message/rfc822".<BR>
 * The reverse-path and the Return-Path header of the response is set to "null" ("<>"),
 * meaning that no reply should be sent.</P>
 * <P>A sender of the notification message can optionally be specified.
 * If one is not specified, the postmaster's address will be used.<BR>
 * <P>Supports the <CODE>passThrough</CODE> init parameter (true if missing).</P>
 *
 * <P>Sample configuration:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="DSNBounce">
 *   &lt;sender&gt;<I>an address or postmaster or sender or unaltered, 
 default=postmaster</I>&lt;/sender&gt;
 *   &lt;prefix&gt;<I>optional subject prefix prepended to the original 
 message</I>&lt;/prefix&gt;
 *   &lt;attachment&gt;<I>message or none, default=message</I>&lt;/attachment&gt;
 *   &lt;messageString&gt;<I>the message sent in the bounce, the first occurrence of the pattern [machine] is replaced with the name of the executing machine, default=Hi. This is the James mail server at [machine] ... </I>&lt;/messageString&gt;
 *   &lt;passThrough&gt;<I>true or false, default=true</I>&lt;/passThrough&gt;
 *   &lt;debug&gt;<I>true or false, default=false</I>&lt;/debug&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 *
 * @see org.apache.james.transport.mailets.AbstractNotify
 */



public class DSNBounce extends AbstractNotify {


    /**
     * Constants and getters for RFC 3463 Enhanced Mail System Status Codes
     *
     * I suggest do extract this inner class for future use in the smtp-handler
     *
     */
    public static class DSNStatus {
        // status code classes
        /**
         * Success
         */
        public static final int SUCCESS = 2;

        /**
         * Persistent Transient Failure
         */
        public static final int TRANSIENT = 4;

        /**
         * Permanent Failure
         */
        public static final int PERMANENT = 5;

        // subjects and details

        /**
         * Other or Undefined Status
         */
        public static final int UNDEFINED = 0;

        /**
         * Other undefined status
         */
        public static final String UNDEFINED_STATUS = "0.0";

        /**
         * Addressing Status
         */
        public static final int ADDRESS = 1;

        /**
         * Other address status
         */
        public static final String ADDRESS_OTHER = "1.0";

        /**
         * Bad destination mailbox address
         */
        public static final String ADDRESS_MAILBOX = "1.1";

        /**
         * Bad destination system address
         */
        public static final String ADDRESS_SYSTEM = "1.2";

        /**
         * Bad destination mailbox address syntax
         */
        public static final String ADDRESS_SYNTAX = "1.3";

        /**
         * Destination mailbox address ambiguous
         */
        public static final String ADDRESS_AMBIGUOUS = "1.4";

        /**
         * Destination Address valid
         */
        public static final String ADDRESS_VALID = "1.5";

        /**
         * Destimation mailbox has moved, no forwarding address
         */
        public static final String ADDRESS_MOVED = "1.6";

        /**
         * Bad sender's mailbox address syntax
         */
        public static final String ADDRESS_SYNTAX_SENDER = "1.7";

        /**
         * Bad sender's system address
         */
        public static final String ADDRESS_SYSTEM_SENDER = "1.8";


        /**
         * Mailbox Status
         */
        public static final int MAILBOX = 2;

        /**
         * Other or Undefined Mailbox Status
         */
        public static final String MAILBOX_OTHER = "2.0";

        /**
         * Mailbox disabled, not accepting messages
         */
        public static final String MAILBOX_DISABLED = "2.1";

        /**
         * Mailbox full
         */
        public static final String MAILBOX_FULL = "2.2";

        /**
         * Message length exceeds administrative limit
         */
        public static final String MAILBOX_MSG_TOO_BIG = "2.3";

        /**
         * Mailing list expansion problem
         */
        public static final String MAILBOX_LIST_EXPANSION = "2.4";


        /**
         * Mail System Status
         */
        public static final int SYSTEM = 3;

        /**
         * Other or undefined mail system status
         */
        public static final String SYSTEM_OTHER = "3.0";

        /**
         * Mail system full
         */
        public static final String SYSTEM_FULL = "3.1";

        /**
         * System not accepting messages
         */
        public static final String SYSTEM_NOT_ACCEPTING = "3.2";

        /**
         * System not capable of selected features
         */
        public static final String SYSTEM_NOT_CAPABLE = "3.3";

        /**
         * Message too big for system
         */
        public static final String SYSTEM_MSG_TOO_BIG = "3.4";

        /**
         * System incorrectly configured
         */
        public static final String SYSTEM_CFG_ERROR = "3.5";


        /**
         * Network and Routing Status
         */
        public static final int NETWORK = 4;

        /**
         * Other or undefined network or routing status
         */
        public static final String NETWORK_OTHER = "4.0";

        /**
         * No answer form host
         */
        public static final String NETWORK_NO_ANSWER = "4.1";

        /**
         * Bad Connection
         */
        public static final String NETWORK_CONNECTION = "4.2";

        /**
         * Directory server failure
         */
        public static final String NETWORK_DIR_SERVER = "4.3";

        /**
         * Unable to route
         */
        public static final String NETWORK_ROUTE = "4.4";

        /**
         * Mail system congestion
         */
        public static final String NETWORK_CONGESTION = "4.5";

        /**
         * Routing loop detected
         */
        public static final String NETWORK_LOOP = "4.6";

        /**
         * Delivery time expired
         */
        public static final String NETWORK_EXPIRED = "4.7";


        /**
         * Mail Delivery Protocol Status
         */
        public static final int DELIVERY = 5;

        /**
         * Other or undefined (SMTP) protocol status
         */
        public static final String DELIVERY_OTHER = "5.0";

        /**
         * Invalid command
         */
        public static final String DELIVERY_INVALID_CMD = "5.1";

        /**
         * Syntax error
         */
        public static final String DELIVERY_SYNTAX = "5.2";

        /**
         * Too many recipients
         */
        public static final String DELIVERY_TOO_MANY_REC = "5.3";

        /**
         * Invalid command arguments
         */
        public static final String DELIVERY_INVALID_ARG = "5.4";

        /**
         * Wrong protocol version
         */
        public static final String DELIVERY_VERSION = "5.5";


        /**
         * Message Content or Media Status
         */
        public static final int CONTENT = 6;

        /**
         * Other or undefined media error
         */
        public static final String CONTENT_OTHER = "6.0";

        /**
         * Media not supported
         */
        public static final String CONTENT_UNSUPPORTED = "6.1";

        /**
         * Conversion required and prohibited
         */
        public static final String CONTENT_CONVERSION_NOT_ALLOWED = "6.2";

        /**
         * Conversion required, but not supported
         */
        public static final String CONTENT_CONVERSION_NOT_SUPPORTED = "6.3";

        /**
         * Conversion with loss performed
         */
        public static final String CONTENT_CONVERSION_LOSS = "6.4";

        /**
         * Conversion failed
         */
        public static final String CONTENT_CONVERSION_FAILED = "6.5";


        /**
         * Security or Policy Status
         */
        public static final int SECURITY = 7;

        /**
         * Other or undefined security status
         */
        public static final String SECURITY_OTHER = "7.0";

        /**
         * Delivery not authorized, message refused
         */
        public static final String SECURITY_AUTH = "7.1";

        /**
         * Mailing list expansion prohibited
         */
        public static final String SECURITY_LIST_EXP = "7.2";

        /**
         * Security conversion required, but not possible
         */
        public static final String SECURITY_CONVERSION = "7.3";

        /**
         * Security features not supported
         */
        public static final String SECURITY_UNSUPPORTED = "7.4";

        /**
         * Cryptographic failure
         */
        public static final String SECURITY_CRYPT_FAIL = "7.5";

        /**
         * Cryptographic algorithm not supported
         */
        public static final String SECURITY_CRYPT_ALGO = "7.6";

        /**
         * Message integrity failure
         */
        public static final String SECURITY_INTEGRITY = "7.7";


        // get methods

        public static String getStatus(int type, String detail) {
            return type + "." + detail;
        }

        public static String getStatus(int type, int subject, int detail) {
            return type + "." + subject + "." + detail;
        }
    }

    private static final RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    //  Used to generate new mail names
    private static final java.util.Random random = new java.util.Random();

    // regexp pattern for scaning status code from exception message
    private static Pattern statusPattern;

    private static Pattern diagPattern;

    private static final String MACHINE_PATTERN = "[machine]";

    private String messageString =
        "Hi. This is the James mail server at [machine].\nI'm afraid I wasn't able to deliver your message to the following addresses.\nThis is a permanent error; I've given up. Sorry it didn't work out.  Below\nI include the list of recipients and the reason why I was unable to deliver\nyour message.\n";

    /*
     * Static initializer.<p>
     * Compiles patterns for processing exception messages.<p>
     */
    static {
        Perl5Compiler compiler = new Perl5Compiler();
        String status_pattern_string = ".*\\s*([245]\\.\\d{1,3}\\.\\d{1,3}).*\\s*";
        String diag_pattern_string = "^\\d{3}\\s.*$";
        try {
            statusPattern = compiler.
                compile(status_pattern_string, Perl5Compiler.READ_ONLY_MASK);
        } catch(MalformedPatternException mpe) {
            //this should not happen as the pattern string is hardcoded.
            System.err.println ("Malformed pattern: " + status_pattern_string);
            mpe.printStackTrace (System.err);
        }
        try {
            diagPattern = compiler.
                compile(diag_pattern_string, Perl5Compiler.READ_ONLY_MASK);
        } catch(MalformedPatternException mpe) {
            //this should not happen as the pattern string is hardcoded.
            System.err.println ("Malformed pattern: " + diag_pattern_string);
        }
    }

    /**
     * Initialize the mailet
     */
    public void init() throws MessagingException {
        super.init();
        if (getInitParameter("messageString") != null) {
            messageString = getInitParameter("messageString");
        }
        
        MailcapCommandMap mail_cap =
            (MailcapCommandMap) CommandMap.getDefaultCommandMap();

        mail_cap.addMailcap ("message/delivery-status;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap (mail_cap);
    }

    /**
     * Service does the hard work and bounces the originalMail in the format specified by RFC3464.
     *
     * @param originalMail the mail to bounce
     * @throws MessagingException if a problem arises formulating the redirected mail
     *
     * @see org.apache.mailet.Mailet#service(org.apache.mailet.Mail)
     */
    public void service(Mail originalMail) throws MessagingException {


        // duplicates the Mail object, to be able to modify the new mail keeping the original untouched
        Mail newMail = ((MailImpl) originalMail).duplicate(newName((MailImpl) originalMail));
        // We don't need to use the original Remote Address and Host,
        // and doing so would likely cause a loop with spam detecting
        // matchers.
        try {
            ((MailImpl)newMail).setRemoteAddr(java.net.InetAddress.getLocalHost().getHostAddress());
            ((MailImpl)newMail).setRemoteHost(java.net.InetAddress.getLocalHost().getHostName());
        } catch (java.net.UnknownHostException _) {
            ((MailImpl) newMail).setRemoteAddr("127.0.0.1");
            ((MailImpl) newMail).setRemoteHost("localhost");
        }

        if (originalMail.getSender() == null) {
            if (isDebug)
                log("Processing a bounce request for a message with an empty reverse-path.  No bounce will be sent.");
            if(!getPassThrough(originalMail)) {
                originalMail.setState(Mail.GHOST);
            }
            return;
        }

        String reversePath = originalMail.getSender().toString();
        if (isDebug)
            log("Processing a bounce request for a message with a reverse path.  The bounce will be sent to " + reversePath);

        Collection newRecipients = new HashSet();
        newRecipients.add(reversePath);
        ((MailImpl)newMail).setRecipients(newRecipients);

        if (isDebug) {
            MailImpl newMailImpl = (MailImpl) newMail;
            log("New mail - sender: " + newMailImpl.getSender()
                + ", recipients: " +
                arrayToString(newMailImpl.getRecipients().toArray())
                + ", name: " + newMailImpl.getName()
                + ", remoteHost: " + newMailImpl.getRemoteHost()
                + ", remoteAddr: " + newMailImpl.getRemoteAddr()
                + ", state: " + newMailImpl.getState()
                + ", lastUpdated: " + newMailImpl.getLastUpdated()
                + ", errorMessage: " + newMailImpl.getErrorMessage());
        }

        // create the bounce message
        MimeMessage newMessage =
            new MimeMessage(Session.getDefaultInstance(System.getProperties(),
                                                       null));

        MimeMultipartReport multipart = new MimeMultipartReport ();
        multipart.setReportType ("delivery-status");
        
        // part 1: descripive text message
        MimeBodyPart part1 = createTextMsg(originalMail);
        multipart.addBodyPart(part1);

        // part 2: DSN
        MimeBodyPart part2 = createDSN(originalMail);
        multipart.addBodyPart(part2);


        // part 3: original mail (optional)
        if (getAttachmentType() != NONE) {
            MimeBodyPart part3 = createAttachedOriginal(originalMail);
            multipart.addBodyPart(part3);
        }


        // stuffing all together
        newMessage.setContent(multipart);
        newMessage.setHeader(RFC2822Headers.CONTENT_TYPE, multipart.getContentType());
        newMail.setMessage(newMessage);

        //Set additional headers
        setRecipients(newMail, getRecipients(originalMail), originalMail);
        setTo(newMail, getTo(originalMail), originalMail);
        setSubjectPrefix(newMail, getSubjectPrefix(originalMail), originalMail);
        if(newMail.getMessage().getHeader(RFC2822Headers.DATE) == null) {
            newMail.getMessage().setHeader(RFC2822Headers.DATE,rfc822DateFormat.format(new Date()));
        }
        setReplyTo(newMail, getReplyTo(originalMail), originalMail);
        setReversePath(newMail, getReversePath(originalMail), originalMail);
        setSender(newMail, getSender(originalMail), originalMail);
        setIsReply(newMail, isReply(originalMail), originalMail);

        newMail.getMessage().saveChanges();
        getMailetContext().sendMail(newMail);

        // ghosting the original mail
        if(!getPassThrough(originalMail)) {
            originalMail.setState(Mail.GHOST);
        }
    }

    /**
     * Create a MimeBodyPart with a textual description for human readers.
     *
     * @param originalMail
     * @return MimeBodyPart
     * @throws MessagingException
     */
    protected MimeBodyPart createTextMsg(Mail originalMail)
        throws MessagingException {
        MimeBodyPart part1 = new MimeBodyPart();
        StringWriter sout = new StringWriter();
        PrintWriter out = new PrintWriter(sout, true);
        String machine = "[unknown]";
        try {
            InetAddress me = InetAddress.getLocalHost();
            machine = me.getHostName();
        } catch(Exception e){
            machine = "[address unknown]";
        }

        StringBuffer bounceBuffer =
            new StringBuffer(128).append (messageString);
        int m_idx_begin = messageString.indexOf(MACHINE_PATTERN);
        if (m_idx_begin != -1) {
            bounceBuffer.replace (m_idx_begin,
                                  m_idx_begin+MACHINE_PATTERN.length(),
                                  machine);
        }
        out.println(bounceBuffer.toString());
        out.println("Failed recipient(s):");
        for (Iterator i = originalMail.getRecipients().iterator(); i.hasNext(); ) {
            out.println(i.next());
        }
        MessagingException ex = (MessagingException)originalMail.getAttribute("delivery-error");
        out.println();
        out.println("Error message:");
        out.println(getErrorMsg(ex));
        out.println();

        part1.setText(sout.toString());
        return part1;
    }

    /**
     * creates the DSN-bodypart for automated processing
     *
     * @param originalMail
     * @return MimeBodyPart dsn-bodypart
     * @throws MessagingException
     */
    protected MimeBodyPart createDSN(Mail originalMail) throws MessagingException {
        MimeBodyPart dsn = new MimeBodyPart();
        MimeMessage dsnMessage =
            new MimeMessage(Session.getDefaultInstance(System.getProperties(), null));
        StringWriter sout = new StringWriter();
        PrintWriter out = new PrintWriter(sout, true);
        String errorMsg = null;
        String nameType = null;


        ////////////////////////
        // per message fields //
        ////////////////////////

        //optional: envelope-id
        // TODO: Envelope-Id
        // The Original-Envelope-Id is NOT the same as the Message-Id from the header.
        // The Message-Id identifies the content of the message, while the Original-Envelope-ID
        // identifies the transaction in which the message is sent.  (see RFC3461)
        // so do NOT out.println("Original-Envelope-Id:"+originalMail.getMessage().getMessageID());


        //required: reporting MTA
        // this is always us, since we do not translate non-internet-mail
        // failure reports into DSNs
        nameType = "dns";
        try {
            String myAddress =
                (String)getMailetContext().getAttribute(Constants.HELLO_NAME);
            /*
            String myAddress = InetAddress.getLocalHost().getCanonicalHostName();
            */
            out.println("Reporting-MTA: "+nameType+"; "+myAddress);
        } catch(Exception e){
            // we should always know our address, so we shouldn't get here
            log("WARNING: sending DSN without required Reporting-MTA Address");
        }

        //only for gateways to non-internet mail systems: dsn-gateway

        //optional: received from
        out.println("Received-From-MTA: "+nameType+"; "+originalMail.getRemoteHost());

        //optional: Arrival-Date

        //////////////////////////
        // per recipient fields //
        //////////////////////////

        Iterator recipients = originalMail.getRecipients().iterator();
        while (recipients.hasNext())
            {
                MailAddress rec = (MailAddress)recipients.next();
                String addressType = "rfc822";

                //required: blank line
                out.println();

                //optional: original recipient (see RFC3461)
                //out.println("Original-Recipient: "+addressType+"; "+ ??? );

                //required: final recipient
                out.println("Final-Recipient: "+addressType+"; "+rec.toString());

                //required: action
                // alowed values: failed, delayed, delivered, relayed, expanded
                // TODO: until now, we do error-bounces only
                out.println("Action: failed");

                //required: status
                // get Exception for getting status information
                // TODO: it would be nice if the SMTP-handler would set a status attribute we can use here
                MessagingException ex =
                    (MessagingException) originalMail.getAttribute("delivery-error");
                out.println("Status: "+getStatus(ex));

                //optional: remote MTA
                //to which MTA were we talking while the Error occured?

                //optional: diagnostic-code
                String diagnosticType = null;
                // this typically is the return value received during smtp
                // (or other transport) communication
                // and should be stored as attribute by the smtp handler
                // but until now we only have error-messages.
                String diagnosticCode = getErrorMsg(ex);
                // Sometimes this is the smtp diagnostic code,
                // but James often gives us other messages
                Perl5Matcher diagMatcher = new Perl5Matcher();
                boolean smtpDiagCodeAvailable =
                    diagMatcher.matches(diagnosticCode, diagPattern);
                if (smtpDiagCodeAvailable){
                    diagnosticType = "smtp";
                } else {
                    diagnosticType = "X-James";
                }
                out.println("Diagnostic-Code: "+diagnosticType+"; "+diagnosticCode);
            
                //optional: last attempt
                out.println("Last-Attempt-Date: "+
                            rfc822DateFormat.format(((MailImpl)originalMail).getLastUpdated()));

                //optional: retry until
                //only for 'delayed' reports .. but we don't report this (at least until now)

                //optional: extension fields

            }


        // setting content
        dsnMessage.setText(sout.toString());
        dsnMessage.saveChanges();

        
        //dsn.setContent(sout.toString(), "text/plain");

        dsn.setContent(dsnMessage, "message/delivery-status");
        dsn.setDescription("Delivery Status Notification");
        dsn.setFileName("status.dat");
        return dsn;
    }

    /**
     * Create a MimeBodyPart with the original Mail as Attachment
     *
     * @param originalMail
     * @return MimeBodyPart
     * @throws MessagingException
     */
    protected MimeBodyPart createAttachedOriginal(Mail originalMail)
        throws MessagingException {
        MimeBodyPart part = new MimeBodyPart();
        MimeMessage originalMessage = originalMail.getMessage();
        part.setContent(originalMessage, "message/rfc822");
        if ((originalMessage.getSubject() != null) && 
            (originalMessage.getSubject().trim().length() > 0)) {
            part.setFileName(originalMessage.getSubject().trim());
        } else {
            part.setFileName("No Subject");
        }
        part.setDisposition("Attachment");
        return part;
    }

    /**
     * Guessing status code by the exception provided.
     * This method should use the status attribute when the
     * SMTP-handler somewhen provides it
     *
     * @param MessagingException
     * @return status code
     */
    protected String getStatus(MessagingException me) {
        if (me.getNextException() == null) {
            String mess = me.getMessage();
            Perl5Matcher m = new Perl5Matcher();
            StringBuffer sb = new StringBuffer();
            if (m.matches(mess, statusPattern)) {
                MatchResult res = m.getMatch();
                sb.append(res.group(1));
                return sb.toString();
            }
            // bad destination system adress
            if (mess.startsWith("There are no DNS entries for the hostname"))
                return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.ADDRESS_SYSTEM);

            // no answer from host (4.4.1) or
            // system not accepting network messages (4.3.2), lets guess ...
            if (mess.equals("No mail server(s) available at this time."))
                return DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.NETWORK_NO_ANSWER);

            // other/unknown error
            return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.UNDEFINED_STATUS);
        } else {
            String retVal = null;
            Exception ex1 = me.getNextException();
            Perl5Matcher m = new Perl5Matcher ();
            StringBuffer sb = new StringBuffer();
            if (m.matches(ex1.getMessage(), statusPattern)) {
                MatchResult res = m.getMatch();
                sb.append(res.group(1));
                return sb.toString();
            } else if (ex1 instanceof SendFailedException) {
                // other/undefined protocol status

                // if we get an smtp returncode starting with 4
                // it is an persistent transient error, else permanent
                if (ex1.getMessage().startsWith("4")) {
                    return DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.DELIVERY_OTHER);
                } else return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_OTHER);
            } else if (ex1 instanceof UnknownHostException) {
                // bad destination system address
                return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.ADDRESS_SYSTEM);
            } else if (ex1 instanceof ConnectException) {
                // bad connection
                return DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.NETWORK_CONNECTION);
            } else if (ex1 instanceof SocketException) {
                // bad connection
                return DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.NETWORK_CONNECTION);
            } else {
                // other/undefined/unknown error
                return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.UNDEFINED_STATUS);
            }
        }
    }

    /**
     * Utility method for getting the error message from the (nested) exception.
     * @param MessagingException
     * @return error message
     */
    protected String getErrorMsg(MessagingException me) {
        if (me.getNextException() == null) {
            return me.getMessage().trim();
        } else {
            Exception ex1 = me.getNextException();
            return ex1.getMessage().trim();
        }
    }

    /**
     * Utility method for obtaining a string representation of an array of Objects.
     */
    private String arrayToString(Object[] array) {
        if (array == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer(1024);
        sb.append("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Create a unique new primary key name.
     *
     * @param mail the mail to use as the basis for the new mail name
     * @return a new name
     */
    protected String newName(MailImpl mail) throws MessagingException {
        String oldName = mail.getName();

        // Checking if the original mail name is too long, perhaps because of a
        // loop caused by a configuration error.
        // it could cause a "null pointer exception" in AvalonMailRepository much
        // harder to understand.
        if (oldName.length() > 76) {
            int count = 0;
            int index = 0;
            while ((index = oldName.indexOf('!', index + 1)) >= 0) {
                count++;
            }
            // It looks like a configuration loop. It's better to stop.
            if (count > 7) {
                throw new MessagingException("Unable to create a new message name: too long."
                                             + " Possible loop in config.xml.");
            }
            else {
                oldName = oldName.substring(0, 76);
            }
        }

        StringBuffer nameBuffer =
            new StringBuffer(64)
            .append(oldName)
            .append("-!")
            .append(random.nextInt(1048576));
        return nameBuffer.toString();
    }



    public String getMailetInfo() {
        return "DSNBounce Mailet";
    }
    /* ******************************************************************** */
    /* ****************** Begin of getX and setX methods ****************** */
    /* ******************************************************************** */

    /** Gets the expected init parameters.  */
    protected  String[] getAllowedInitParameters() {
        String[] allowedArray = {
            "debug",
            "passThrough",
            "messageString",
            "attachment",
            "sender",
            "prefix"
        };
        return allowedArray;
    }

    /**
     * @return the <CODE>attachment</CODE> init parameter, or <CODE>MESSAGE</CODE> if missing
     */
    protected int getAttachmentType() throws MessagingException {
        if(getInitParameter("attachment") == null) {
            return MESSAGE;
        } else {
            return getTypeCode(getInitParameter("attachment"));
        }
    }


    /**
     * @return <CODE>SpecialAddress.REVERSE_PATH</CODE>
     */
    protected Collection getRecipients() {
        Collection newRecipients = new HashSet();
        newRecipients.add(SpecialAddress.REVERSE_PATH);
        return newRecipients;
    }

    /**
     * @return <CODE>SpecialAddress.REVERSE_PATH</CODE>
     */
    protected InternetAddress[] getTo() {
        InternetAddress[] apparentlyTo = new InternetAddress[1];
        apparentlyTo[0] = SpecialAddress.REVERSE_PATH.toInternetAddress();
        return apparentlyTo;
    }

    /**
     * @return <CODE>SpecialAddress.NULL</CODE> (the meaning of bounce)
     */
    protected MailAddress getReversePath(Mail originalMail) {
        return SpecialAddress.NULL;
    }

    /* ******************************************************************** */
    /* ******************* End of getX and setX methods ******************* */
    /* ******************************************************************** */

}
