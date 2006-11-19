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

package org.apache.james.imapserver.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.ParseException;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.mailet.MailAddress;
import org.apache.mailet.dates.RFC822DateFormat;

/**
 * Attributes of a Message in IMAP4rev1 style. Message
 * Attributes should be set when a message enters a mailbox.
 * <p> Note that the message in a mailbox have the same order using either
 * Message Sequence Numbers or UIDs.
 * <p> reinitialize() must be called on deserialization to reset Logger
 *
 * Reference: RFC 2060 - para 2.3
 */
public class SimpleMessageAttributes
    extends AbstractLogEnabled
    implements ImapMessageAttributes
{

    private final static String SP = " ";
    private final static String NIL = "NIL";
    private final static String Q = "\"";
    private final static String LB = "(";
    private final static String RB = ")";
    private final static boolean DEBUG = false;
    private final static String MULTIPART = "MULTIPART";
    private final static String MESSAGE = "MESSAGE";

    private int uid;
    private int messageSequenceNumber;
    private Date internalDate;
    private String internalDateString;
    private String bodyStructure;
    private String envelope;
    private int size;
    private int lineCount;
    public ImapMessageAttributes[] parts;
    private List headers;

    //rfc822 or MIME header fields
    //arrays only if multiple values allowed under rfc822
    private String subject;
    private String[] from;
    private String[] sender;
    private String[] replyTo;
    private String[] to;
    private String[] cc;
    private String[] bcc;
    private String[] inReplyTo;
    private String[] date;
    private String[] messageID;
    private String contentType;
    private String primaryType;   // parsed from contentType
    private String secondaryType; // parsed from contentType
    private Set parameters;      // parsed from contentType
    private String contentID;
    private String contentDesc;
    private String contentEncoding;

    SimpleMessageAttributes() {
    }
    
    public SimpleMessageAttributes(MimeMessage mm, Logger logger) throws MessagingException {
        enableLogging(logger);
        setAttributesFor(mm);
    }
    
    public SimpleMessageAttributes(MimeMessage mm) throws MessagingException {
        setAttributesFor(mm);
    }

    void setAttributesFor(MimeMessage msg) throws MessagingException {
        size = msg.getSize();
        try {
            internalDate = msg.getSentDate();
        } catch (MessagingException me) {
        }
        if (internalDate == null) {
            // TODO setAttributesFor: decide what to do when internalDate is null
            internalDate=new Date();
        }

        internalDateString = RFC822DateFormat.toString(internalDate); // not right format
        parseMimePart(msg);
        envelope = null;
        bodyStructure = null;
    }

    void setUID(int thisUID) {
        uid = thisUID;
    }

    /**
     * Parses key data items from a MimeMessage for seperate storage.
     * TODO this is a mess, and should be completely revamped.
     */
    void parseMimePart(MimePart part) {
        // Section 1 - Message Headers
        if (part instanceof MimeMessage) {
            try {
                subject = ((MimeMessage)part).getSubject();
            } catch (MessagingException me) {
                if (DEBUG) getLogger().debug("Messaging Exception for getSubject: " + me);
            }
        }
        try {
            from = part.getHeader("From");
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(From): " + me);
        }
        try {
            sender = part.getHeader("Sender");
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(Sender): " + me);
        }
        try {
            replyTo = part.getHeader("Reply To");
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(Reply To): " + me);
        }
        try {
            to = part.getHeader("To");
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(To): " + me);
        }
        try {
            cc = part.getHeader("Cc");
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(To): " + me);
        }
        try {
            bcc = part.getHeader("Bcc");
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(To): " + me);
        }
        try {
            inReplyTo = part.getHeader("In Reply To");
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(In Reply To): " + me);
        }
        try {
            date = part.getHeader("Date");
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(Date): " + me);
        }
        try {
            messageID = part.getHeader("Message-ID");
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(messageID): " + me);
        }
        String contentTypeLine = null;
        try {
            contentTypeLine = part.getContentType();
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getContentType(): " + me);
        }
        if (contentTypeLine !=null ) {
            decodeContentType(contentTypeLine);
        }
        try {
            contentID = part.getContentID();
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getContentUD(): " + me);
        }
        try {
            contentDesc = part.getDescription();
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getDescription(): " + me);
        }
        try {
            contentEncoding = part.getEncoding();
            // default value.
            if ( contentEncoding == null ) {
                contentEncoding = "7BIT";
            }
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getEncoding(): " + me);
        }
        if (DEBUG) {
            try {
                String contentDisposition = part.getDisposition();
            } catch (MessagingException me) {
                getLogger().debug("Messaging Exception for getEncoding(): " + me);
            }
        }

        try {
            // TODO this doesn't work
            lineCount = part.getLineCount();
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getLineCount(): " + me);
        } catch (Exception e) {
            if (DEBUG) getLogger().debug("Exception for getLineCount(): " + e);
        }

        // Recurse through any embedded parts
        if (primaryType.equalsIgnoreCase(MULTIPART)) {
            MimeMultipart container;
            try {
                container =(MimeMultipart) part.getContent();
                int count = container.getCount();
                parts = new SimpleMessageAttributes[count];
                for (int i = 0; i < count ; i ++) {
                    BodyPart nextPart = container.getBodyPart(i);

                    if (nextPart instanceof MimePart) {
                        SimpleMessageAttributes partAttrs = new SimpleMessageAttributes();
                        setupLogger(partAttrs); // reset transient logger
                        partAttrs.parseMimePart((MimePart)nextPart);
                        parts[i] = partAttrs;

                    } else {
                        getLogger().info("Found a non-Mime bodyPart");
                    }
                }
            } catch (Exception e) {
                getLogger().debug("Messaging Exception for getContent(): " + e);
                e.printStackTrace();
            }
        } else if (primaryType.equalsIgnoreCase("message")) {
            getLogger().info("This part contains an embedded message of subtype: " + secondaryType);
            getLogger().info("Uses java class: " + part.getClass().getName());
            if (secondaryType.equalsIgnoreCase("RFC822")) {
                //try {

                    /*
                    MimeMessageWrapper message = new MimeMessageWrapper(part.getInputStream());
                    SimpleMessageAttributes msgAttrs = new SimpleMessageAttributes();
                    msgAttrs.setAttributesFor(message);

                    if (part instanceof MimeMessage) {
                        Comments out because I don't know what it should do here
                        MimeMessage msg1 = (MimeMessage) part;
                        MimeMessageWrapper message2 = new MimeMessageWrapper(msg1);
                        SimpleMessageAttributes msgAttrs2 = new SimpleMessageAttributes();
                        msgAttrs.setAttributesFor(message2);
                    }

                    parts = new SimpleMessageAttributes[1];
                    parts[0] = msgAttrs;
                    */
                //} catch (Exception e) {
                //getLogger().error("Error interpreting a message/rfc822: " + e);
                //e.printStackTrace();
                //}
            } else {
                getLogger().info("Unknown subtype of message encountered.");
                System.out.println("Unknown subtype of message encountered.");
            }
        }
        else {
//            System.out.println("parseMimePart: its just a plain message");
        }
    }

    /**
     * Builds IMAP envelope String from pre-parsed data.
     */
    String parseEnvelope() {
        List response = new ArrayList();
        response.add( LB + Q + internalDateString + Q + SP);
        if (subject != null && (!subject.equals(""))) {
            response.add( Q +  subject + Q + SP );
        } else {
            response.add( NIL + SP );
        }
        if (from != null && from.length > 0) {
            response.add(LB);
            for (int i=0; i<from.length; i++) {
                response.add(parseAddress( from[i]) );
            }
            response.add(RB);
        } else {
            response.add( NIL);
        }
        response.add(SP);
        if (sender != null && sender.length >0) {
            if (DEBUG) getLogger().debug("parsingEnvelope - sender[0] is: " + sender[0]);
            //Check for Netscape feature - sender is local part only
            if (sender[0].indexOf("@") == -1) {
                response.add(LB + (String)response.get(3) + RB); //first From address
            } else {
                response.add(LB);
                for (int i=0; i<sender.length; i++) {
                    response.add( parseAddress(sender[i]));
                }
                response.add(RB);
            }
        } else {
            if (from != null && from.length > 0) {
                response.add(LB + (String)response.get(3) + RB); //first From address
            } else {
                response.add( NIL);
            }
        }
        response.add(SP);
        if (replyTo != null && replyTo.length >0) {
            if (replyTo[0].indexOf("@") == -1) {
                response.add(LB + (String)response.get(3) + RB); //first From address
            } else {
                response.add(LB);
                for (int i=0; i<replyTo.length; i++) {
                    response.add( parseAddress(replyTo[i]));
                }
                response.add(RB);
            }
        } else {
            if (from != null && from.length > 0) {
                response.add(LB + (String)response.get(3) + RB); //first From address
            } else {
                response.add( NIL);
            }
        }
        response.add(SP);
        if (to != null && to.length >0) {
            response.add(LB);
            for (int i=0; i<to.length; i++) {
                response.add( parseAddress(to[i]));
            }
            response.add(RB);
        } else {
            response.add( NIL);
        }
        response.add(SP);
        if (cc != null && cc.length >0) {
            response.add(LB);
            for (int i=0; i<cc.length; i++) {
                response.add( parseAddress(cc[i]));
            }
            response.add(RB);
        } else {
            response.add( NIL);
        }
        response.add(SP);
        if (bcc != null && bcc.length >0) {
            response.add(LB);
            for (int i=0; i<bcc.length; i++) {
                response.add( parseAddress(bcc[i]));
            }
            response.add(RB);
        } else {
            response.add( NIL);
        }
        response.add(SP);
        if (inReplyTo != null && inReplyTo.length>0) {
            response.add( inReplyTo[0]);
        } else {
            response.add( NIL);
        }
        response.add(SP);
        if (messageID != null && messageID.length>0) {
            response.add(Q + messageID[0] + Q);
        } else {
            response.add( NIL);
        }
        response.add(RB);

        StringBuffer buf = new StringBuffer(16 * response.size());
        for (int j=0; j<response.size(); j++) {
            buf.append((String)response.get(j));
        }

        return buf.toString();
    }

    /**
     * Parses a String email address to an IMAP address string.
     */
    String parseAddress(String address) {
        int comma = address.indexOf(",");
        StringBuffer buf = new StringBuffer();
        if (comma == -1) { //single address
            buf.append(LB);
            InternetAddress netAddr = null;
            try {
                netAddr = new InternetAddress(address);
            } catch (AddressException ae) {
                return null;
            }
            String personal = netAddr.getPersonal();
            if (personal != null && (!personal.equals(""))) {
                buf.append(Q + personal + Q);
            } else {
                buf.append( NIL);
            }
            buf.append( SP);
            buf.append( NIL) ; // should add route-addr
            buf.append( SP);
            try {
                MailAddress mailAddr = new MailAddress(netAddr);
                buf.append(Q + mailAddr.getUser() + Q);
                buf.append(SP);
                buf.append(Q + mailAddr.getHost() + Q);
            } catch (ParseException pe) {
                buf.append( NIL + SP + NIL);
            }
            buf.append(RB);
        } else {
            buf.append(parseAddress(address.substring(0, comma)));
            buf.append(SP);
            buf.append(parseAddress(address.substring(comma + 1)));
        }
        return buf.toString();
    }

    /**
     * Decode a content Type header line into types and parameters pairs
     */
    void decodeContentType(String rawLine) {
        int slash = rawLine.indexOf("/");
        if( slash == -1){
            if (DEBUG) getLogger().debug("decoding ... no slash found");
            return;
        } else {
            primaryType = rawLine.substring(0, slash).trim();
        }
        int semicolon = rawLine.indexOf(";");
        if (semicolon == -1) {
            if (DEBUG) getLogger().debug("decoding ... no semicolon found");
            secondaryType = rawLine.substring(slash + 1).trim();
            return;
        }
        // have parameters
        parameters = new HashSet();
        secondaryType = rawLine.substring(slash + 1, semicolon).trim();
        int pos = semicolon;
        int nextsemi = rawLine.indexOf(";", pos+1);
        while (nextsemi != -1) {
            if (DEBUG) getLogger().debug("decoding ... found another semicolon");
            String param = rawLine.substring(pos + 1, nextsemi);
            int esign = param.indexOf("=") ;
            if (esign == -1) {
                if (DEBUG) getLogger().debug("Whacky parameter found: " + param);
            } else {
                String name = param.substring(0, esign).trim();
                String value = param.substring(esign + 1).trim();
                parameters.add(name + SP + value);
                if (DEBUG) getLogger().debug("Found parameter: " + name + SP + value);
            }
            pos = nextsemi;
            nextsemi = rawLine.indexOf(";", pos +1);
        }
        String lastParam = rawLine.substring(pos + 1);
        int esign = lastParam.indexOf("=") ;
        if (esign == -1) {
            if (DEBUG) getLogger().debug("Whacky parameter found: " + lastParam);
        } else {
            String name = lastParam.substring(0, esign).trim();
            String value = lastParam.substring(esign + 1).trim();
            parameters.add(Q + name + Q + SP + Q + value + Q);
            if (DEBUG) getLogger().debug("Found parameter: " + name + SP + value);
        }
    }

    String parseBodyFields() {
        StringBuffer buf = new StringBuffer();
        if (parameters == null || parameters.isEmpty()) {
            buf.append(NIL);
        } else {
            buf.append(LB);
            Iterator it = parameters.iterator();
            while(it.hasNext()) {
                buf.append((String)it.next());
            }
            buf.append(RB);
        }
        buf.append(SP);
        if(contentID == null) {
            buf.append(NIL);
        } else {
            buf.append(Q + contentID + Q);
        }
        buf.append(SP);
        if(contentDesc == null) {
            buf.append(NIL);
        } else {
            buf.append(Q + contentDesc + Q);
        }
        buf.append(SP);
        if(contentEncoding == null) {
            buf.append( NIL );
        } else {
            buf.append(Q + contentEncoding + Q);
        }
        buf.append(SP);
        buf.append(size);
        return buf.toString();
    }

    /**
     * Produce the IMAP formatted String for the BodyStructure of a pre-parsed MimeMessage
     * TODO handle extension elements - Content-disposition, Content-Language and other parameters.
     */
    String parseBodyStructure() {
        try {
            String fields = parseBodyFields();
            StringBuffer buf = new StringBuffer();
            buf.append(LB);
            if (primaryType.equalsIgnoreCase("Text")) {
                buf.append("\"TEXT\" \"" );
                buf.append( secondaryType.toUpperCase() );
                buf.append( "\" ");
                buf.append( fields );
                buf.append( " " );
                buf.append( lineCount );

                // is:    * 1 FETCH (BODYSTRUCTURE ("Text" "plain" NIL NIL NIL NIL    4  -1))
                // wants: * 1 FETCH (BODYSTRUCTURE ("text" "plain" NIL NIL NIL "8bit" 6  1  NIL NIL NIL))
                // or:    * 1 FETCH (BODYSTRUCTURE ("text" "plain" NIL NIL NIL "7bit" 28 1 NIL NIL NIL))

            } else if  (primaryType.equalsIgnoreCase(MESSAGE) && secondaryType.equalsIgnoreCase("rfc822")) {
                buf.append("\"MESSAGE\" \"RFC822\" ");
                buf.append(fields + SP);
                setupLogger(parts[0]); // reset transient logger
                buf.append(parts[0].getEnvelope() + SP);
                buf.append(parts[0].getBodyStructure( false ) + SP);
                buf.append(lineCount);
            } else if (primaryType.equalsIgnoreCase(MULTIPART)) {
                for (int i=0; i<parts.length; i++) {
                    setupLogger(parts[i]); // reset transient getLogger()
                    buf.append(parts[i].getBodyStructure( false ));
                }
                buf.append(SP + secondaryType);
            }
            buf.append(RB);
            return buf.toString();
        } catch (Exception e) {
            getLogger().error("Exception while parsing BodyStrucuture: " + e);
            e.printStackTrace();
            throw new RuntimeException("Exception in parseBodyStructure");
        }
    }

    /**
     * Provides the current Message Sequence Number for this message. MSNs
     * change when messages are expunged from the mailbox.
     *
     * @return int a positive non-zero integer
     */
    public int getMessageSequenceNumber() {
        return messageSequenceNumber;
    }

    void setMessageSequenceNumber(int newMsn) {
        messageSequenceNumber = newMsn;
    }


    /**
     * Provides the unique identity value for this message. UIDs combined with
     * a UIDValidity value form a unique reference for a message in a given
     * mailbox. UIDs persist across sessions unless the UIDValidity value is
     * incremented. UIDs are not copied if a message is copied to another
     * mailbox.
     *
     * @return int a 32-bit value
     */
    public int getUID() {
        return uid;
    }

    /**
     * Provides the date and time at which the message was received. In the
     * case of delivery by SMTP, this SHOULD be the date and time of final
     * delivery as defined for SMTP. In the case of messages copied from
     * another mailbox, it shuld be the internalDate of the source message. In
     * the case of messages Appended to the mailbox, example drafts,  the
     * internalDate is either specified in the Append command or is the
     * current dat and time at the time of the Append.
     *
     * @return Date imap internal date
     */
    public Date getInternalDate() {
        return internalDate;
    }

    public String getInternalDateAsString() {
        return internalDateString;
    }

    /**
     * Provides the sizeof the message in octets.
     *
     * @return int number of octets in message.
     */
    public int getSize() {
        return size;
    }

    /**
     * Provides the Envelope structure information for this message. This is a parsed representation of the rfc-822 envelope information. This is not to be confused with the SMTP envelope!
     *
     * @return String satisfying envelope syntax in rfc 2060.
     */
    public String getEnvelope() {
        return parseEnvelope();
    }

    /**
     * Provides the Body Structure information for this message. This is a parsed representtion of the MIME structure of the message.
     *
     * @return String satisfying body syntax in rfc 2060.
     */
    public String getBodyStructure( boolean includeExtensions ) {
        return parseBodyStructure();
    }
}
