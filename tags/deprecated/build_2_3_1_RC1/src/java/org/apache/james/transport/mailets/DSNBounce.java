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

package org.apache.james.transport.mailets;

import org.apache.james.Constants;
import org.apache.james.core.MailImpl;
import org.apache.james.util.mail.MimeMultipartReport;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.dates.RFC822DateFormat;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

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
 *   &lt;attachment&gt;<I>message, heads or none, default=message</I>&lt;/attachment&gt;
 *   &lt;messageString&gt;<I>the message sent in the bounce, the first occurrence of the pattern [machine] is replaced with the name of the executing machine, default=Hi. This is the James mail server at [machine] ... </I>&lt;/messageString&gt;
 *   &lt;passThrough&gt;<I>true or false, default=true</I>&lt;/passThrough&gt;
 *   &lt;debug&gt;<I>true or false, default=false</I>&lt;/debug&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 *
 * @see org.apache.james.transport.mailets.AbstractNotify
 */



public class DSNBounce extends AbstractNotify {


    private static final RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    //  Used to generate new mail names
    private static final java.util.Random random = new java.util.Random();

    // regexp pattern for scaning status code from exception message
    private static Pattern statusPattern;

    private static Pattern diagPattern;

    private static final String MACHINE_PATTERN = "[machine]";

    private String messageString = null;

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
        messageString = getInitParameter("messageString","Hi. This is the James mail server at [machine].\nI'm afraid I wasn't able to deliver your message to the following addresses.\nThis is a permanent error; I've given up. Sorry it didn't work out.  Below\nI include the list of recipients and the reason why I was unable to deliver\nyour message.\n");
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
        MailImpl newMail = new MailImpl(originalMail,newName(originalMail));
        try {
            // We don't need to use the original Remote Address and Host,
            // and doing so would likely cause a loop with spam detecting
            // matchers.
            try {
                newMail.setRemoteAddr(java.net.InetAddress.getLocalHost().getHostAddress());
                newMail.setRemoteHost(java.net.InetAddress.getLocalHost().getHostName());
            } catch (java.net.UnknownHostException _) {
                newMail.setRemoteAddr("127.0.0.1");
                newMail.setRemoteHost("localhost");
            }
    
            if (originalMail.getSender() == null) {
                if (isDebug)
                    log("Processing a bounce request for a message with an empty reverse-path.  No bounce will be sent.");
                if(!getPassThrough(originalMail)) {
                    originalMail.setState(Mail.GHOST);
                }
                return;
            }
    
            MailAddress reversePath = originalMail.getSender();
            if (isDebug)
                log("Processing a bounce request for a message with a reverse path.  The bounce will be sent to " + reversePath);
    
            Collection newRecipients = new HashSet();
            newRecipients.add(reversePath);
            newMail.setRecipients(newRecipients);
    
            if (isDebug) {
                log("New mail - sender: " + newMail.getSender()
                    + ", recipients: " +
                    arrayToString(newMail.getRecipients().toArray())
                    + ", name: " + newMail.getName()
                    + ", remoteHost: " + newMail.getRemoteHost()
                    + ", remoteAddr: " + newMail.getRemoteAddr()
                    + ", state: " + newMail.getState()
                    + ", lastUpdated: " + newMail.getLastUpdated()
                    + ", errorMessage: " + newMail.getErrorMessage());
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
                MimeBodyPart part3 = createAttachedOriginal(originalMail,getAttachmentType());
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
        } finally {
            newMail.dispose();
        }

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
        StringWriter sout = new StringWriter();
        PrintWriter out = new PrintWriter(sout, true);
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
                            rfc822DateFormat.format(originalMail.getLastUpdated()));

                //optional: retry until
                //only for 'delayed' reports .. but we don't report this (at least until now)

                //optional: extension fields

            }


        // Changed this from rfc822 handling to text/plain
        // It should be handled correctly as delivery-status but it
        // is better text/plain than rfc822 (rfc822 add message headers not
        // allowed in the delivery-status.
        // text/plain does not support rfc822 header encoding that we
        // should support here.
        dsn.setContent(sout.toString(), "text/plain");
        dsn.setHeader("Content-Type","message/delivery-status");
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
    protected MimeBodyPart createAttachedOriginal(Mail originalMail, int attachmentType)
        throws MessagingException {
        MimeBodyPart part = new MimeBodyPart();
        MimeMessage originalMessage = originalMail.getMessage();
        
        if (attachmentType == HEADS) {
            part.setContent(getMessageHeaders(originalMessage), "text/plain");
            part.setHeader("Content-Type","text/rfc822-headers");
        } else {
            part.setContent(originalMessage, "message/rfc822");
        }
        
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
            Exception ex1 = me.getNextException();
            Perl5Matcher m = new Perl5Matcher ();
            StringBuffer sb = new StringBuffer();
            if (m.matches(ex1.getMessage(), statusPattern)) {
                MatchResult res = m.getMatch();
                sb.append(res.group(1));
                return sb.toString();
            } else if (ex1 instanceof SendFailedException) {
                // other/undefined protocol status
                int smtpCode = 0;
                try {
                    smtpCode = Integer.parseInt(ex1.getMessage().substring(0,3));
                } catch(NumberFormatException e) {
                }

                switch (smtpCode) {
                
                    // Req mail action not taken: mailbox unavailable
                    case 450: return DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.MAILBOX_OTHER);
                    // Req action aborted: local error in processing
                    case 451: return DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.SYSTEM_OTHER);
                    // Req action not taken: insufficient sys storage
                    case 452: return DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.SYSTEM_FULL);
                    // Syntax error, command unrecognized
                    case 500: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_SYNTAX);
                    // Syntax error in parameters or arguments
                    case 501: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_ARG);
                    // Command not implemented
                    case 502: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_CMD);
                    // Bad sequence of commands
                    case 503: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_CMD);
                    // Command parameter not implemented
                    case 504: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_ARG);
                    // Req mail action not taken: mailbox unavailable
                    case 550: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.MAILBOX_OTHER);
                    // User not local; please try <...>
                    // 5.7.1 Select another host to act as your forwarder
                    case 551: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.SECURITY_AUTH);
                    // Req mail action aborted: exceeded storage alloc
                    case 552: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.MAILBOX_FULL);
                    // Req action not taken: mailbox name not allowed
                    case 553: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.ADDRESS_SYNTAX);
                    // Transaction failed
                    case 554: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.UNDEFINED_STATUS);
                    // Not authorized. This is not an SMTP code, but many server use it.
                    case 571: return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.SECURITY_AUTH);
                    
                    default:
                        // if we get an smtp returncode starting with 4
                        // it is an persistent transient error, else permanent
                        if (ex1.getMessage().startsWith("4")) {
                            return DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.DELIVERY_OTHER);
                        } else return DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_OTHER);
                }

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
    protected String newName(Mail mail) throws MessagingException {
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
        return getTypeCode(getInitParameter("attachment","message"));
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
