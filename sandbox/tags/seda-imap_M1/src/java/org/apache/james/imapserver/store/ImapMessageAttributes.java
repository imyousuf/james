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


