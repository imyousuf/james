/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import java.io.*;
import java.util.*;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.*;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.util.RFC822DateFormat;
import org.apache.log.Logger;
import org.apache.mailet.*;

/**
 * Attributes of a Message in IMAP4rev1 style. Message
 * Attributes should be set when a message enters a mailbox.
 * <p> Note that the message in a mailbox have the same order using either
 * Message Sequence Numbers or UIDs.
 * <p> reinitialize() must be called on deserialization to reset Logger
 *
 * Reference: RFC 2060 - para 2.3
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class SimpleMessageAttributes
    extends AbstractLoggable
    implements MessageAttributes, Serializable  {

    private final static String SP = " ";
    private final static String NIL = "NIL";
    private final static String Q = "\"";
    private final static String LB = "(";
    private final static String RB = ")";
    private final static boolean DEBUG = true;
    private final static String MULTIPART = "MULTIPART";
    private final static String MESSAGE = "MESSAGE";

    //Only available in first incarnation of object
    private transient Logger logger;

    private int uid;
    private int messageSequenceNumber;
    private Date internalDate;
    private String internalDateString;
    private String bodyStructure;
    private String envelope;
    private int size;
    private int lineCount;
    private MessageAttributes[] parts;
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

    void setAttributesFor(MimeMessage msg) throws MessagingException {
        size = msg.getSize();

        try {
            internalDate = msg.getSentDate();
            if (DEBUG) getLogger().debug("setAttributes - getSentDate: " + internalDate);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getSentDate: " + me);
            internalDate = new Date();
        }

        if (DEBUG) {
            getLogger().debug("HeaderLines recieved were: ");
            Enumeration enum = msg.getAllHeaderLines();
            while(enum.hasMoreElements()) {
                getLogger().debug((String)enum.nextElement());
            }
            //  getLogger().debug("Header objects available are:");
            //   Enumeration e = msg.getAllHeaders();
            //   while(e.hasMoreElements()) {
            //Header h = (Header) e.nextElement();
            //getLogger().debug("Name: " + h.getName());
            //getLogger().debug("Value: " + h.getValue());
            //  }
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
     */
    void parseMimePart(MimePart part) {
        // Section 1 - Message Headers
        if (part instanceof MimeMessage) {
            try {
                subject = ((MimeMessage)part).getSubject();
                if (DEBUG) getLogger().debug("parseMessage - subject: " + subject);
            } catch (MessagingException me) {
                if (DEBUG) getLogger().debug("Messaging Exception for getSubject: " + me);
            }
        }
        try {
            from = part.getHeader("From");
            if (DEBUG)  getLogger().debug("parseMessage - from: " + from);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(From): " + me);
        }
        try {
            sender = part.getHeader("Sender");
            if (DEBUG) getLogger().debug("parseMessage - sender: " + sender);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(Sender): " + me);
        }
        try {
            replyTo = part.getHeader("Reply To");
            if (DEBUG) getLogger().debug("parseMessage - ReplyTo: " + replyTo);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(Reply To): " + me);
        }
        try {
            to = part.getHeader("To");
            if (DEBUG) getLogger().debug("parseMessage - To: " + to);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(To): " + me);
        }
        try {
            cc = part.getHeader("Cc");
            if (DEBUG) getLogger().debug("parseMessage - cc: " + cc);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(To): " + me);
        }
        try {
            bcc = part.getHeader("Bcc");
            if (DEBUG) getLogger().debug("parseMessage - bcc: " + bcc);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(To): " + me);
        }
        try {
            inReplyTo = part.getHeader("In Reply To");
            if (DEBUG) getLogger().debug("parseMessage - In Reply To: " + inReplyTo);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(In Reply To): " + me);
        }
        try {
            date = part.getHeader("Date");
            if (DEBUG) getLogger().debug("parseMessage - date: " + date);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(Date): " + me);
        }
        try {
            messageID = part.getHeader("Message-ID");
            if (DEBUG) getLogger().debug("parseMessage - messageID: " + messageID);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(messageID): " + me);
        }
        String contentTypeLine = null;
        try {
            contentTypeLine = part.getContentType();
            if (DEBUG) getLogger().debug("parseMessage - contentType: " + contentTypeLine);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getContentType(): " + me);
        }
        if (contentTypeLine !=null ) {
            decodeContentType(contentTypeLine);
        }
        try {
            contentID = part.getContentID();
            if (DEBUG) getLogger().debug("parseMessage - contentID: " + contentID);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getContentUD(): " + me);
        }
        try {
            contentDesc = part.getDescription();
            if (DEBUG) getLogger().debug("parseMessage - contentDesc: " + contentDesc);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getDescription(): " + me);
        }
        try {
            contentEncoding = part.getEncoding();
            if (DEBUG) getLogger().debug("parseMessage - contentEncoding: " + contentEncoding);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getEncoding(): " + me);
        }
        if (DEBUG) {
            try {
                String contentDisposition = part.getDisposition();
                getLogger().debug("parseMessage - contentDisposition: " + contentEncoding);
            } catch (MessagingException me) {
                getLogger().debug("Messaging Exception for getEncoding(): " + me);
            }
        }

        try {
            lineCount = part.getLineCount();
            if (DEBUG) getLogger().debug("parseMessage - Line Count: " + lineCount);
        } catch (MessagingException me) {
            if (DEBUG) getLogger().debug("Messaging Exception for getLineCount(): " + me);
            if (DEBUG) getLogger().debug(me.getMessage());
        } catch (Exception e) {
            if (DEBUG) getLogger().debug("Exception for getLineCount(): " + e);
            if (DEBUG) getLogger().debug("Exception message was: " +  e.getMessage());
        }

        // Recurse through any embedded parts
        if (primaryType.equalsIgnoreCase(MULTIPART)) {
            MimeMultipart container;
            try {
                container =(MimeMultipart) part.getContent();
                int count = container.getCount();
                getLogger().info("This part contains " + count + " parts.");
                parts = new SimpleMessageAttributes[count];
                for (int i = 0; i < count ; i ++) {
                    getLogger().info("Getting embedded part: " + i);
                    BodyPart nextPart = container.getBodyPart(i);

                    if (nextPart instanceof MimePart) {
                        SimpleMessageAttributes partAttrs = new SimpleMessageAttributes();
                        partAttrs.parseMimePart((MimePart)nextPart);
                        parts[i] = partAttrs;

                    } else {
                        getLogger().info("Found a non-Mime bodyPart");
                    }
                    getLogger().info("Finished with embedded part: " + i);
                }
            } catch (Exception e) {
                getLogger().debug("Messaging Exception for getContent(): " + e);
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
            }
            getLogger().info("Finished with embedded message. " );
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
        getLogger().info("Parsing address: " + address);
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
        if (DEBUG) getLogger().debug("decoding: " + rawLine);
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
        getLogger().debug("Parsing body fields");
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
            buf.append(NIL);
        } else {
            buf.append(Q + contentEncoding + Q);
        }
        buf.append(SP);
        buf.append(size);
        return buf.toString();
    }

    /**
     * Produce the IMAP formatted String for the BodyStructure of a pre-parsed MimeMessage
     */
    String parseBodyStructure() {
        getLogger().debug("Parsing bodyStructure.");
        try {
            String fields = parseBodyFields();
            StringBuffer buf = new StringBuffer();
            buf.append(LB);
            if (primaryType.equalsIgnoreCase("Text")) {
                getLogger().debug("Assembling bodystrucuture for type TEXT.");
                buf.append("\"Text\" \"" + secondaryType + "\" ");
                buf.append(fields + " " + lineCount);
            } else if  (primaryType.equalsIgnoreCase(MESSAGE) && secondaryType.equalsIgnoreCase("rfc822")) {
                getLogger().debug("Assembling bodyStructure for type MESSAGE/FRC822");
                buf.append("\"MESSAGE\" \"RFC822\" ");
                buf.append(fields + SP);
                setupLogger(parts[0]); // reset transient logger
                buf.append(parts[0].getEnvelope() + SP);
                buf.append(parts[0].getBodyStructure() + SP);
                buf.append(lineCount);
            } else if (primaryType.equalsIgnoreCase(MULTIPART)) {
                getLogger().debug("Assembling bodystructure for type MULTIPART");
                for (int i=0; i<parts.length; i++) {
                    getLogger().debug("Parsing part: " + i);
                    setupLogger(parts[i]); // reset transient getLogger()
                    buf.append(parts[i].getBodyStructure());
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
     * @returns int a positive non-zero integer
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
     * @returns int a 32-bit value
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
     * @returns Date imap internal date
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
     * @returns int number of octets in message.
     */
    public int getSize() {
        return size;
    }

    /**
     * Provides the Envelope structure information for this message. This is a parsed representation of the rfc-822 envelope information. This is not to be confused with the SMTP envelope!
     *
     * @returns String satisfying envelope syntax in rfc 2060.
     */
    public String getEnvelope() {
        return parseEnvelope();
    }


    /**
     * Provides the Body Structure information for this message. This is a parsed representtion of the MIME structure of the message.
     *
     * @returns String satisfying body syntax in rfc 2060.
     */
    public String getBodyStructure() {
        return parseBodyStructure();
    }
}
