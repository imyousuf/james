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

package org.apache.james.imapserver.store;

import java.util.Date;

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
public interface ImapMessageAttributes  {

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
    Date getInternalDate();

    /**
     * Returns IMAP formatted String representation of Date
     */
    String getInternalDateAsString();

    /**
     * Provides the sizeof the message in octets.
     *
     * @return int number of octets in message.
     */
    int getSize();

    /**
     * Provides the Envelope structure information for this message. 
     * This is a parsed representation of the rfc-822 envelope information. 
     * This is not to be confused with the SMTP envelope!
     *
     * @return String satisfying envelope syntax in rfc 2060.
     */
    String getEnvelope();

    /**
     * Provides the Body Structure information for this message. 
     * This is a parsed representtion of the MIME structure of the message.
     *
     * @return String satisfying body syntax in rfc 2060.
     */
    String getBodyStructure( boolean includeExtensions );
}


