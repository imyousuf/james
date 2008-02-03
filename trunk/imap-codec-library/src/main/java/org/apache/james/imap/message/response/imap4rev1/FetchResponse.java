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
package org.apache.james.imap.message.response.imap4rev1;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.api.imap.message.response.ImapResponseMessage;

public final class FetchResponse implements ImapResponseMessage {

    private final int messageNumber;
    private final Flags flags;
    private final Long uid;
    private final Date internalDate;
    private final Integer size;
    private final StringBuffer misc;
    private final List elements;
    private final Envelope envelope;
    
    public FetchResponse(final int messageNumber, final Flags flags, final Long uid,
            final Date internalDate, final Integer size, final Envelope envelope,
            StringBuffer misc, List elements) {
        super();
        this.messageNumber = messageNumber;
        this.flags = flags;
        this.uid = uid;
        this.internalDate = internalDate;
        this.size = size;
        this.envelope = envelope;
        this.misc = misc;
        this.elements = elements;
    }

    /**
     * Gets the number of the message whose details 
     * have been fetched.
     * @return message number
     */
    public final int getMessageNumber() {
        return messageNumber;
    }

    /**
     * Gets the fetched flags.
     * @return {@link Flags} fetched,
     * or null if the <code>FETCH</code> did not include <code>FLAGS</code>
     */
    public Flags getFlags() {
        return flags;
    }

    /**
     * Gets the unique id for the fetched message.
     * @return message uid, 
     * or null if the <code>FETCH</code> did not include <code>UID</code>
     */
    public Long getUid() {
        return uid;
    }

    /**
     * Gets the internal date for the fetched message.
     * @return the internalDate,
     * or null if the <code>FETCH</code> did not include <code>INTERNALDATE</code>
     */
    public final Date getInternalDate() {
        return internalDate;
    }

    /**
     * Gets the size for the fetched message.
     * @return the size,
     * or null if the <code>FETCH</code> did not include <code>SIZE</code>
     */
    public final Integer getSize() {
        return size;
    }
    
    /**
     * Gets the envelope for the fetched message
     * @return the envelope,
     * or null if the <code>FETCH</code> did not include <code>ENVELOPE</code>
     */
    public final Envelope getEnvelope() {
        return envelope;
    }

    /**
     * TODO: replace
     * @return <code>List</code> of <code>BodyElement</code>'s, 
     * or null if the <code>FETCH</code> did not include body elements
     */
    public final List getElements() {
        return elements;
    }

    /**
     * TODO: replace
     * @return the misc
     */
    public final StringBuffer getMisc() {
        return misc;
    }
    
    /**
     * BODY FETCH element content.
     */
    public interface BodyElement extends Literal {
        
        /**
         * The full name of the element fetched.
         * As per <code>FETCH</code> command input.
         * @return name, not null
         */
        public String getName();
        
        /**
         * Size of the literal content data.
         * @return number of octets which {@link #writeTo(WritableByteChannel)}
         * will put onto the channel
         */
        public long size();
        
        /**
         * Writes the contents of this body element to the channel.
         * @param channel <code>Channel</code>, not null
         * @throws IOException
         */
        public void writeTo(WritableByteChannel channel) throws IOException;
    }
    
    /**
     * ENVELOPE content.
     */
    public interface Envelope {
        
        /**
         * Gets the envelope <code>date</code>.
         * This is the value of the RFC822 <code>date</code> header.
         * @return envelope Date
         * or null if this attribute is <code>NIL</code>
         */
        public String getDate();
        
        /**
         * Gets the envelope <code>subject</code>.
         * This is the value of the RFC822 <code>subject</code> header.
         * @return subject,
         * or null if this attribute is <code>NIL</code>
         */
        public String getSubject();
        
        /**
         * Gets the envelope <code>from</code> addresses.
         * 
         * @return from addresses, not null
         */
        public Address[] getFrom();
        
        /**
         * Gets the envelope <code>sender</code> addresses.
         * @return <code>sender</code> addresses, not null
         */
        public Address[] getSender();
     
        /**
         * Gets the envelope <code>reply-to</code> addresses.
         * @return <code>reply-to</code>, not null
         */
        public Address[] getReplyTo();
        
        /**
         * Gets the envelope <code>to</code> addresses.
         * @return <code>to</code>,
         * or null if <code>NIL</code>
         */
        public Address[] getTo();
        
        /**
         * Gets the envelope <code>cc</code> addresses.
         * @return <code>cc</code>, 
         * or null if <code>NIL</code>
         */
        public Address[] getCc();
        
        /**
         * Gets the envelope <code>bcc</code> addresses.
         * @return <code>bcc</code>,
         * or null if <code>NIL</code>
         */
        public Address[] getBcc();
        
        /**
         * Gets the envelope <code>in-reply-to</code>.
         * @return <code>in-reply-to</code>
         * or null if <code>NIL</code>
         */
        public String getInReplyTo();
        
        /**
         * Gets the envelope <code>message
         * @return
         */
        public String getMessageId();
        
        /**
         * Values an envelope address.
         */
        public interface Address {
            
            /** Empty array */
            public static final Address[] EMPTY = {};
            
            /**
             * Gets the personal name.
             * @return personal name, 
             * or null if the personal name is <code>NIL</code>
             */
            public String getPersonalName();
            
            /**
             * Gets the SMTP source route.
             * @return SMTP at-domain-list, 
             * or null if the list if <code>NIL</code>
             */
            public String getAtDomainList();
            
            /**
             * Gets the mailbox name.
             * @return the mailbox name 
             * or the group name when {@link #getHostName()}
             * is null
             */
            public String getMailboxName();
            
            /**
             * Gets the host name.
             * @return the host name,
             * or null when this address marks the start
             * or end of a group
             */
            public String getHostName();
        }
    }
}
