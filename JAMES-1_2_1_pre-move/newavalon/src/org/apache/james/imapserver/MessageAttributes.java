/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imapserver;

//import java.io.*;
//import java.net.*;
import java.util.Date;
//import javax.mail.*;
//import javax.mail.internet.*;

/**
 * Interface for objects holding IMAP4rev1 Message Attributes. Message
 * Attributes should be set when a message enters a mailbox. Implementations
 * are encouraged to implement and store MessageAttributes apart from the
 * underlying message. This allows the Mailbox to respond to questions about
 * very large message without needing to access them directly.
 * <p> Note that the message in a mailbox have the same order using either
 * Message Sequence Numbers or UIDs.
 *
 * Reference: RFC 2060 - para 2.3
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */

public interface MessageAttributes  {

    /**
     * Provides the current Message Sequence Number for this message. MSNs
     * change when messages are expunged from the mailbox.
     *
     * @returns int a positive non-zero integer
     */
    public int getMessageSequenceNumber();

    /**
     * Provides the unique identity value for this message. UIDs combined with
     * a UIDValidity value form a unique reference for a message in a given
     * mailbox. UIDs persist across sessions unless the UIDValidity value is
     * incremented. UIDs are not copied if a message is copied to another
     * mailbox.
     *
     * @returns int a 32-bit value
     */
    public int getUID();


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
    public Date getInternalDate();

    /**
     * Returns IMAP formatted String representation of Date
     */
    public String getInternalDateAsString();


    /**
     * Provides the sizeof the message in octets.
     *
     * @returns int number of octets in message.
     */
    public int getSize();

    /**
     * Provides the Envelope structure information for this message. This is a parsed representation of the rfc-822 envelope information. This is not to be confused with the SMTP envelope!
     *
     * @returns String satisfying envelope syntax in rfc 2060.
     */
    public String getEnvelope();

    /**
     * Provides the Body Structure information for this message. This is a parsed representtion of the MIME structure of the message.
     *
     * @returns String satisfying body syntax in rfc 2060.
     */
    public String getBodyStructure();



}


