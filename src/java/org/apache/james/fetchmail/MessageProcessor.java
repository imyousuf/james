/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */
 
package org.apache.james.fetchmail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * <p>Class <code>MessageProcessor</code> handles the delivery of 
 * <code>MimeMessages</code> to the James input spool.</p>
 * 
 * <p>Messages written to the input spool always have the following Mail
 * Attributes set:</p>
 * <dl>
 * <dt>org.apache.james.fetchmail.taskName (java.lang.String)</dt>
 *      <dd>The name of the fetch task that processed the message</dd>
 * <dt>org.apache.james.fetchmail.folderName (java.lang.String)</dt>
 *      <dd>The name of the folder from which the message was fetched</dd>
 * </dl>
 * 
 * <p>Messages written to the input spool have the following Mail Attributes
 *  set if the corresponding condition is satisfied:
 * <dl>
 * <dt>org.apache.james.fetchmail.isRemoteRecipient (java.lang.Boolean)</dt>
 *      <dd>The recipient is on a remote host</dd>
 * <dt>org.apache.james.fetchmail.isUserUndefined (java.lang.Boolean)</dt>
 *      <dd>The recipient is on a localhost but not defined to James</dd>
 * <dt>org.apache.james.fetchmail.isBlacklistedRecipient (java.lang.Boolean)</dt>
 *      <dd>The recipient is in the configured blacklist</dd>
 * <dt>org.apache.james.fetchmail.isRecipientNotFound (java.lang.Boolean)</dt>
 *      <dd>The recipient could not be found. Delivery is to the configured recipient. 
 *          See the discussion of delivery to a sole intended recipient below.</dd>
 * </dl>
 * 
 * <p>Configuration settings -
 *  see <code>org.apache.james.fetchmail.ParsedConfiguration</code>
 * - control the messages that are written to the James input spool, those that
 * are rejected and what happens to messages that are rejected.</p>
 * 
 * <p>Rejection processing is based on the following filters:</p>
 * <dl> 
 * <dt>RejectRemoteRecipient</dt> 
 *      <dd>Rejects recipients on remote hosts</dd>
 * <dt>RejectBlacklistedRecipient</dt>
 *      <dd>Rejects recipients configured in a blacklist</dd>
 * <dt>RejectUserUndefined</dt>
 *      <dd>Rejects recipients on local hosts who are not defined as James users</dd>
 * <dt>RejectRecipientNotFound</dt>
 *      <dd>See the discussion of delivery to a sole intended recipient below</dd>
 * </dl>
 * 
 * <p>Rejection processing is intentionally limited to managing the status of the
 * messages that are rejected on the server from which they were fetched. View
 * it as a simple automation of the manual processing an end-user would perform 
 * through a mail client. Messages may be marked as seen or be deleted.</p> 
 * 
 * <p>Further processing can be achieved by configuring to disable rejection for 
 * one or more filters. This enables Messages that would have been rejected to
 * be written to the James input spool. The conditional Mail Attributes 
 * described above identify the filter states. The Matcher/Mailet chain can 
 * then be used to perform any further processing required, such as notifying
 * the Postmaster and/or sender, marking the message for error processing, etc.</p>
 * 
 * <p>Delivery is to a sole intended recipient. The recipient is determined in the
 * following manner:</p>
 *
 * <ol> 
 * <li>If isIgnoreIntendedRecipient(), use the configured recipient</li>
 * <li>If the Envelope contains a for: stanza, use the recipient in the stanza</li>
 * <li>If the Message has a sole intended recipient, use this recipient</li>
 * <li>If not rejectRecipientNotFound(), use the configured recipient</li>
 * </ol>
 * 
 * <p>If a recipient cannot be determined after these steps, the message is 
 * rejected.</p>
 * 
 * <p>Every delivered message CURRENTLY has an "X-fetched-from" header added 
 * containing the name of the fetch task. Its primary uses are to detect bouncing
 * mail and provide backwards compatibility with the fetchPop task that inserted
 * this header to enable injected messages to be detected in the Matcher/Mailet 
 * chain. This header is DEPRECATED and WILL BE REMOVED in a future version of 
 * fetchmail. Use the Mail Attribute <code>org.apache.james.fetchmail.taskName</code>
 * instead.
 * 
 * <p><code>MessageProcessor</code> is as agnostic as it can be about the format
 * and contents of the messages it delivers. There are no RFCs that govern its
 * behavior. The most releveant RFCs relate to the exchange of messages between
 * MTA servers, but not POP3 or IMAP servers which are normally end-point
 * servers and not expected to re-inject mail into MTAs. None the less, the
 * intent is to conform to the 'spirit' of the RFCs.
 * <code>MessageProcessor</code> relies on the MTA (James in this
 * implementation) to manage and validate the injected mail just as it would
 * when receiving mail from an upstream MTA.</p> 
 * 
 * <p>The only correction applied by <code>MessageProcessor</code> is to correct a
 * partial originator address. If the originator address has a valid user part
 * but no domain part, a domain part is added. The added domain is either the 
 * default domain specified in the configuration, or if not specified, the 
 * fully qualified name of the machine on which the fetch task is running.</p>
 * 
 * <p>The status of messages on the server from which they were fetched that 
 * cannot be injected into the input spool due to non-correctable errors is 
 * determined by the undeliverable configuration options.</p>
 * 
 * <p>Creation Date: 27-May-03</p>
 *
 */
public class MessageProcessor extends ProcessorAbstract
{
    private MimeMessage fieldMessageIn;

    /**
     * Recipient cannot be found
     */ 
    private boolean fieldRecipientNotFound = false;

    /**
     * Recipient is a local user on a local host
     */ 
    private boolean fieldRemoteRecipient = true;

    /**
     * Recipient is not a local user
     */ 
    private boolean fieldUserUndefined = false;
    
    /**
     * Recipient is blacklisted
     */ 
    private boolean fieldBlacklistedRecipient = false;
    
    /**
     * Constructor for MessageProcessor.
     * 
     * @param configuration
     */
    private MessageProcessor(ParsedConfiguration configuration)
    {
        super(configuration);
    }
    
    /**
     * Constructor for MessageProcessor.
     * 
     * @param messageIn
     * @param configuration
     */

    MessageProcessor(MimeMessage messageIn, ParsedConfiguration configuration)
    {
        this(configuration);
        setMessageIn(messageIn);
    }   

    
    /**
     * Method process attempts to deliver a fetched message.
     * 
     * @see org.apache.james.fetchmail.ProcessorAbstract#process()
     */
    public void process() throws MessagingException
    {
        // Log delivery attempt
        StringBuffer logMessageBuffer =
            new StringBuffer("Attempting delivery of message with id. ");
        logMessageBuffer.append(getMessageIn().getMessageID());
        getLogger().info(logMessageBuffer.toString());

        // Determine the intended recipient
        MailAddress intendedRecipient = getIntendedRecipient();
        setRecipientNotFound(null == intendedRecipient);

        if (isRecipientNotFound())
        {
            if (isRejectRecipientNotFound())
            {
                rejectRecipientNotFound();
                return;
            }
            intendedRecipient = getRecipient();
            StringBuffer messageBuffer =
                new StringBuffer("Intended recipient not found. Using configured recipient as new envelope recipient - ");
            messageBuffer.append(intendedRecipient.toString());
            getLogger().info(messageBuffer.toString());
        }

        // Set the filter states
        setRemoteRecipient(!isLocalServer(intendedRecipient));
        setUserUndefined(!isLocalRecipient(intendedRecipient));
        setBlacklistedRecipient(isBlacklistedRecipient(intendedRecipient));

        // Check recipient against blacklist / whitelist
        // Return if rejected
        if (isRejectRemoteRecipient() & isRemoteRecipient())
        {
            rejectRemoteRecipient(intendedRecipient);
            return;
        }

        if (isRejectUserUndefined() & isUserUndefined())
        {
            rejectUserUndefined(intendedRecipient);
            return;
        }

        if (isRejectBlacklisted() & isBlacklistedRecipient())
        {
            rejectBlacklistedRecipient(intendedRecipient);
            return;
        }

        // Create the mail
        // If any of the addresses are malformed, we will get a
        // ParseException. In which case, we log the problem and
        // return. The message will be left unaltered on the server.
        Mail mail = null;
        try
        {
            mail = createMail(createMessage(), intendedRecipient);
        }
        catch (ParseException ex)
        {
            handleParseException(ex, intendedRecipient);
            return;
        }

        createMailAttributes(mail);

        // OK, lets try and send that mail  
        sendMail(mail);
    }

    /**
     * Method rejectRemoteRecipient.
     * @param recipient
     * @throws MessagingException
     */
    protected void rejectRemoteRecipient(MailAddress recipient)
        throws MessagingException
    {
        // Update the flags of the received message
        if (!isLeaveRemoteRecipient())
            getMessageIn().setFlag(Flags.Flag.DELETED, true);

        if (isMarkRemoteRecipientSeen())
            getMessageIn().setFlag(Flags.Flag.SEEN, true);

        StringBuffer messageBuffer =
            new StringBuffer("Rejected mail intended for remote recipient: ");
        messageBuffer.append(recipient.toString());
        getLogger().info(messageBuffer.toString());

        return;
    }

    /**
     * Method rejectBlacklistedRecipient.
     * @param recipient
     * @throws MessagingException
     */
    protected void rejectBlacklistedRecipient(MailAddress recipient)
        throws MessagingException
    {
        // Update the flags of the received message
        if (!isLeaveBlacklisted())
            getMessageIn().setFlag(Flags.Flag.DELETED, true);

        if (isMarkBlacklistedSeen())
            getMessageIn().setFlag(Flags.Flag.SEEN, true);

        StringBuffer messageBuffer =
            new StringBuffer("Rejected mail intended for blacklisted recipient: ");
        messageBuffer.append(recipient.toString());
        getLogger().info(messageBuffer.toString());

        return;
    }

    /**
     * Method rejectRecipientNotFound.
     * @throws MessagingException
     */
    protected void rejectRecipientNotFound() throws MessagingException
    {
        // Update the flags of the received message
        if (!isLeaveRecipientNotFound())
            getMessageIn().setFlag(Flags.Flag.DELETED, true);

        if (isMarkRecipientNotFoundSeen())
            getMessageIn().setFlag(Flags.Flag.SEEN, true);

        StringBuffer messageBuffer =
            new StringBuffer("Rejected mail for which a sole intended recipient could not be found. Recipients: ");
        Address[] allRecipients = getMessageIn().getAllRecipients();
        for (int i = 0; i < allRecipients.length; i++)
        {
            messageBuffer.append(allRecipients[i].toString());
            messageBuffer.append(' ');
        }   
        getLogger().info(messageBuffer.toString());
        return;
    }
    
    /**
     * Method rejectUserUndefined.
     * @param recipient
     * @throws MessagingException
     */
    protected void rejectUserUndefined(MailAddress recipient)
        throws MessagingException
    {
        // Update the flags of the received message
        if (!isLeaveUserUndefined())
            getMessageIn().setFlag(Flags.Flag.DELETED, true);

        if (isMarkUserUndefinedSeen())
            getMessageIn().setFlag(Flags.Flag.SEEN, true);

        StringBuffer messageBuffer =
            new StringBuffer("Rejected mail intended for undefined user: ");
        messageBuffer.append(recipient.toString());
        getLogger().info(messageBuffer.toString());

        return;
    }   
    
    /**
     * Method createMessage answers a new <code>MimeMessage</code> from the
     * fetched message.
     * 
     * @return MimeMessage
     * @throws MessagingException
     */
    protected MimeMessage createMessage() throws MessagingException
    {
        // Create a new messsage from the received message
        MimeMessage messageOut = new MimeMessage(getMessageIn());

        // set the X-fetched headers
        // Note this is still required to detect bouncing mail and
        // for backwards compatibility with fetchPop 
        messageOut.addHeader("X-fetched-from", getFetchTaskName());
        
        return messageOut;       
    }   

    /**
     * Method createMail creates a new <code>Mail</code>.
     * 
     * @param message
     * @param recipient
     * @return Mail
     * @throws MessagingException
     */
    protected Mail createMail(MimeMessage message, MailAddress recipient)
        throws MessagingException
    {
        Collection recipients = new ArrayList(1);
        recipients.add(recipient);
        return new MailImpl(
            getServer().getId(),
            getSender(),
            recipients,
            message);
    }

    /**
     * Method getSender answers a <code>MailAddress</code> for the sender.
     * 
     * @return MailAddress
     * @throws MessagingException
     */
    protected MailAddress getSender() throws MessagingException
    {
        String from =
            ((InternetAddress) getMessageIn().getFrom()[0]).getAddress().trim();
        InternetAddress internetAddress = null;

        // Check for domain part, add default if missing
        if (from.indexOf('@') < 0)
        {
            StringBuffer fromBuffer = new StringBuffer(from);
            fromBuffer.append('@');
            fromBuffer.append(getDefaultDomainName());
            internetAddress = new InternetAddress(fromBuffer.toString());
        }
        else
            internetAddress = new InternetAddress(from);

        return new MailAddress(internetAddress);
    }
    
    /**
     * Method handleBouncing sets the Mail state to ERROR.
     * 
     * @param mail
     */
    protected void handleBouncing(Mail mail)
    {
        mail.setState(Mail.ERROR);
        mail.setErrorMessage(
            "This mail from FetchMail task "
                + getFetchTaskName()
                + " seems to be bouncing!");
        getLogger().error(
            "A message from FetchMail task "
                + getFetchTaskName()
                + " seems to be bouncing! Moved to Error repository");
    }
    
    /**
     * Method handleParseException.
     * @param ex
     * @param intendedRecipient
     * @throws MessagingException
     */
    protected void handleParseException(
        ParseException ex,
        MailAddress intendedRecipient) throws MessagingException
    {
        // Update the flags of the received message
        if (!isLeaveUndeliverable())
            getMessageIn().setFlag(Flags.Flag.DELETED, true);
    
        if (isMarkUndeliverableSeen())
            getMessageIn().setFlag(Flags.Flag.SEEN, true);
                    
        getLogger().error(
            getFetchTaskName()
                + " Message could not be processed due to an error parsing the addresses."
                + " Subject: "
                + getMessageIn().getSubject()
                + " From: "
                + getMessageIn().getFrom()[0].toString()
                + " To: "
                + intendedRecipient.toString());
        getLogger().debug(ex.toString());
//      ex.printStackTrace();
    }       

    /**
     * Method isLocalRecipient.
     * @param recipient
     * @return boolean
     */
    protected boolean isLocalRecipient(MailAddress recipient)
    {
        return isLocalUser(recipient) && isLocalServer(recipient);
    }
    
    /**
     * Method isLocalServer.
     * @param recipient
     * @return boolean
     */
    protected boolean isLocalServer(MailAddress recipient)
    {
        return getServer().isLocalServer(recipient.getHost());
    }
    
    /**
     * Method isLocalUser.
     * @param recipient
     * @return boolean
     */
    protected boolean isLocalUser(MailAddress recipient)
    {
        return getLocalUsers().containsCaseInsensitive(recipient.getUser());
    }       
    
    /**
     * Method isBlacklistedRecipient.
     * @param recipient
     * @return boolean
     */
    protected boolean isBlacklistedRecipient(MailAddress recipient)
    {
        return getBlacklist().contains(recipient);
    }       

    /**
     * Check if this mail has been bouncing by counting the X-fetched-from headers
     * 
     * @return boolean
     */  
    protected boolean isBouncing() throws MessagingException
    {
        Enumeration enum =
            getMessageIn().getMatchingHeaderLines(
                new String[] { "X-fetched-from" });
        int count = 1;
        while (enum.hasMoreElements())
        {
            enum.nextElement();
            count++;
        }
        return count > 3;
    }
    
    /**
     * Method sendMail.
     * @param mail
     * @throws MessagingException
     */
    protected void sendMail(Mail mail) throws MessagingException
    {
        // If this mail is bouncing move it to the ERROR repository
        //     else
        // Send the mail
        if (isBouncing())
            handleBouncing(mail);
        else
        {
            getServer().sendMail(mail);
            getLogger().info(
                "Spooled message to " + mail.getRecipients().toString());
            getLogger().debug("Sent message " + mail.getMessage().toString());
        }

        // Update the flags of the received message
        if (!isLeave())
            getMessageIn().setFlag(Flags.Flag.DELETED, true);

        if (isMarkSeen())
            getMessageIn().setFlag(Flags.Flag.SEEN, true);
    }   


    /**
     * Method getEnvelopeRecipient answers the recipient if found else null.
     * 
     * Try and parse the "for" parameter from a Received header
     * Maybe not the most accurate parsing in the world but it should do
     * I opted not to use ORO (maybe I should have)
     * 
     * @param msg
     * @return String
     */

    protected String getEnvelopeRecipient(MimeMessage msg)
    {
        try
        {
            Enumeration enum =
                msg.getMatchingHeaderLines(new String[] { "Received" });
            while (enum.hasMoreElements())
            {
                String received = (String) enum.nextElement();

                int nextSearchAt = 0;
                int i = 0;
                int start = 0;
                int end = 0;
                boolean hasBracket = false;
                boolean usableAddress = false;
                while (!usableAddress && (i != -1))
                {
                    hasBracket = false;
                    i = received.indexOf("for ", nextSearchAt);
                    if (i > 0)
                    {
                        start = i + 4;
                        end = 0;
                        nextSearchAt = start;
                        for (int c = start; c < received.length(); c++)
                        {
                            char ch = received.charAt(c);
                            switch (ch)
                            {
                                case '<' :
                                    hasBracket = true;
                                    continue;
                                case '@' :
                                    usableAddress = true;
                                    continue;
                                case ' ' :
                                    end = c;
                                    break;
                                case ';' :
                                    end = c;
                                    break;
                            }
                            if (end > 0)
                                break;
                        }
                    }
                }
                if (usableAddress)
                {
                    // lets try and grab the email address
                    String mailFor = received.substring(start, end);

                    // strip the <> around the address if there are any
                    if (mailFor.startsWith("<") && mailFor.endsWith(">"))
                        mailFor = mailFor.substring(1, (mailFor.length() - 1));

                    return mailFor;
                }
            }
        }
        catch (MessagingException me)
        {
            getLogger().warn("No Received headers found");
        }
        return null;
    }
    
    /**
     * Method getIntendedRecipient answers the sole intended recipient else null.
     * 
     * @return MailAddress
     * @throws MessagingException
     */
    protected MailAddress getIntendedRecipient() throws MessagingException
    {
        // If the original recipient should be ignored, answer the 
        // hard-coded recipient
        if (isIgnorelRecipientHeader())
        {
            StringBuffer messageBuffer =
                new StringBuffer("Ignoring recipient header. Using configured recipient as new envelope recipient - ");
            messageBuffer.append(getRecipient().toString());
            getLogger().info(messageBuffer.toString());
            return getRecipient();
        }

        // If we can determine who the message was received for, answer
        // the target recipient
        String targetRecipient = getEnvelopeRecipient(getMessageIn());
        if (targetRecipient != null)
        {
            MailAddress recipient = new MailAddress(targetRecipient);
            StringBuffer messageBuffer =
                new StringBuffer("Using original envelope recipient as new envelope recipient - ");
            messageBuffer.append(recipient.toString());
            getLogger().info(messageBuffer.toString());
            return recipient;
        }

        // If we can determine the intended recipient from all of the recipients,
        // answer the intended recipient. This requires that there is exactly one
        // recipient answered by getAllRecipients(), which examines the TO: CC: and
        // BCC: headers
        Address[] allRecipients = getMessageIn().getAllRecipients();
        if (allRecipients.length == 1)
        {
            MailAddress recipient =
                new MailAddress((InternetAddress) allRecipients[0]);
            StringBuffer messageBuffer =
                new StringBuffer("Using sole recipient header address as new envelope recipient - ");
            messageBuffer.append(recipient.toString());
            getLogger().info(messageBuffer.toString());
            return recipient;
        }

        return null;
    }           

    /**
     * Returns the messageIn.
     * @return MimeMessage
     */
    protected MimeMessage getMessageIn()
    {
        return fieldMessageIn;
    }

    /**
     * Sets the messageIn.
     * @param messageIn The messageIn to set
     */
    protected void setMessageIn(MimeMessage messageIn)
    {
        fieldMessageIn = messageIn;
    }

    /**
     * Returns the localRecipient.
     * @return boolean
     */
    protected boolean isRemoteRecipient()
    {
        return fieldRemoteRecipient;
    }

    /**
     * Returns the userUndefined.
     * @return boolean
     */
    protected boolean isUserUndefined()
    {
        return fieldUserUndefined;
    }

    /**
     * Returns the Blacklisted.
     * @return boolean
     */
    protected boolean isBlacklistedRecipient()
    {
        return fieldBlacklistedRecipient;
    }

    /**
     * Sets the localRecipient.
     * @param localRecipient The localRecipient to set
     */
    protected void setRemoteRecipient(boolean localRecipient)
    {
        fieldRemoteRecipient = localRecipient;
    }

    /**
     * Sets the userUndefined.
     * @param userUndefined The userUndefined to set
     */
    protected void setUserUndefined(boolean userUndefined)
    {
        fieldUserUndefined = userUndefined;
    }

    /**
     * Creates the mail attributes on a <code>Mail</code>. 
     * @param aMail a Mail instance
     */
    protected void createMailAttributes(Mail aMail)
    {
        aMail.setAttribute(
            getAttributePrefix() + "taskName",
            getFetchTaskName());

        aMail.setAttribute(
            getAttributePrefix() + "folderName",
            getMessageIn().getFolder().getFullName());

        if (isRemoteRecipient())
            aMail.setAttribute(
                getAttributePrefix() + "isRemoteRecipient",
                null);

        if (isUserUndefined())
            aMail.setAttribute(getAttributePrefix() + "isUserUndefined", null);

        if (isBlacklistedRecipient())
            aMail.setAttribute(
                getAttributePrefix() + "isBlacklistedRecipient",
                null);

        if (isRecipientNotFound())
            aMail.setAttribute(
                getAttributePrefix() + "isRecipientNotFound",
                null);
    }     

    /**
     * Sets the Blacklisted.
     * @param blacklisted The blacklisted to set
     */
    protected void setBlacklistedRecipient(boolean blacklisted)
    {
        fieldBlacklistedRecipient = blacklisted;
    }

    /**
     * Returns the recipientNotFound.
     * @return boolean
     */
    protected boolean isRecipientNotFound()
    {
        return fieldRecipientNotFound;
    }

    /**
     * Sets the recipientNotFound.
     * @param recipientNotFound The recipientNotFound to set
     */
    protected void setRecipientNotFound(boolean recipientNotFound)
    {
        fieldRecipientNotFound = recipientNotFound;
    }

}
