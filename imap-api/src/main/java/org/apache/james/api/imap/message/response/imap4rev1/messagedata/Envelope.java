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

package org.apache.james.api.imap.message.response.imap4rev1.messagedata;

import org.apache.james.api.imap.message.response.Addresses;

/**
 * <p>Envelope structure defined by RFC2060.</p> 
 *
 */
public interface Envelope {
    
    /**
     * Gets the date header in RFC882 format.
     * @return <code>CharSequence</code>,
     * or null if this message does not have a <code>date</code>
     * header
     */
    public CharSequence getDate();
    
    /**
     * Gets the <code>subject</code> header value.
     * @return <code>CharSequence</code>,
     * or null if this message does not have a <code>subject</code>
     * header
     */
    public CharSequence getSubject();
    
    /**
     * Gets the addresses comprising the <code>from</code>
     * header value.
     * @return <code>CharSequence</code>,
     * or null if this message does not have a <code>from</code>
     * header
     */
    public Addresses getFrom();
    
    /**
     * Gets the addresses comprising the <code>sender</code>
     * header value.
     * @return <code>CharSequence</code>,
     * or null if this message does not have a <code>sender</code>
     * header
     */
    public Addresses getSender();
    
    /**
     * Gets the addresses comprising the <code>reply-to</code>
     * header value.
     * @return <code>CharSequence</code>,
     * or null if this message does not have a <code>reply-to</code>
     * header
     */
    public Addresses getReplyTo();
    
    /**
     * Gets the addresses comprising the <code>to</code>
     * header value.
     * @return <code>CharSequence</code>,
     * or null if this message does not have a <code>to</code>
     * header
     */
    public Addresses getTo();
    
    /**
     * Gets the addresses comprising the <code>cc</code>
     * header value.
     * @return <code>CharSequence</code>,
     * or null if this message does not have a <code>cc</code>
     * header
     */
    public Addresses getCc();
    
    /**
     * Gets the addresses comprising the <code>bcc</code>
     * header value.
     * @return <code>CharSequence</code>,
     * or null if this message does not have a <code>bcc</code>
     * header
     */
    public Addresses getBcc();
    
    /**
     * Gets the <code>in-reply-to</code> header value.
     * @return <code>CharSequence</code>,
     * or null if this message does not have a <code>in-reply-to</code>
     * header
     */
    public CharSequence getInReplyTo();
    
    /**
     * Gets the <code>message-id</code> header value.
     * @return <code>CharSequence</code>,
     * or null if this message does not have a <code>message-id</code>
     * header
     */
    public CharSequence getMessageId();
}
