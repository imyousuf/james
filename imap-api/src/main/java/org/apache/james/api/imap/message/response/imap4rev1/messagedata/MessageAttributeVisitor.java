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

import java.util.Date;

import org.apache.james.api.imap.message.response.Flags;
import org.apache.james.api.imap.message.response.Literal;

/**
 * Visits RFC2060 msg_att.
 *
 */
public interface MessageAttributeVisitor {
    /**
     * RFC2060 envelope data.
     * @param envelope, <code>Envelope</code> describing 
     * the message, not null
     */
    public void visitEnvelope(Envelope envelope);
    
    /**
     * RFC2060 flag data.
     * @param flags <code>Flags</code>, not null
     */
    public void visitFlags(Flags flags);
    
    /**
     * RFC2060 internal date message attribute.
     * This is the internal server date and time stamp.
     * Defined by section 2.3.3.
     * @param date <code>Date</code>, not null
     */
    public void visitInternalDate(Date date);
    
    /**
     * RFC2060 complete RFC822 message data.
     * @param message <code>Literal</code> message data,
     * not null
     */
    public void visitRfc822(Literal message);
    
    /**
     * RFC2060 header data.
     * @param message <code>Literal</code> header data,
     * not null
     */
    public void visitRfc822Header(Literal headers);
    
    /**
     * RFC2060 message text data.
     * @param message <code>Literal</code> message text,
     * not null
     */
    public void visitRfc822Text(Literal text);
    
    /**
     * RFC2060 message size.
     * @param messageSize the size of the message as defined in
     * RFC822
     */
    public void visitRfc822Size(long messageSize);
    
    /**
     * RFC body section message data for complete message.
     * @param sectionContents <code>Literal</code> contents,
     * not null
     */
    public void visitBodySection(Literal sectionContents);
    
    /**
     * RFC body section message data a part of the complete message.
     * @param originOctet the data starts at the this byte count
     * @param sectionContents <code>Literal</code> contents,
     * not null
     */
    public void visitBodySection(long originOctet, Literal sectionContents);
    
    
    /**
     * RFC body section message data for given section.
     * @param section <code>Section</code>, not null
     * @param sectionContents <code>Literal</code> contents,
     * not null
     */
    public void visitBodySection(Section section, Literal sectionContents);
    
    /**
     * RFC body section message data for given section
     * with given origin octet.
     * @param section <code>Section</code>, not null
     * @param originOctet the data starts at the this byte count
     * @param sectionContents <code>Literal</code> contents,
     * not null
     */
    public void visitBodySection(Section section, long originOctet, Literal sectionContents);
    
    /**
     * The unique indentifier for the message.
     * @param uid unique identifier for the message. This is 
     * an unsigned 32 bit integer represented as a signed 32 bit
     * integer
     */
    public void visitUid(int uid); 
    
    /**
     * RFC2060 <code>body</code>.
     * @param body <code>Body</code>, not null
     * @param structure true if the body contains structure data 
     */
    public void visitBody(Body body, boolean structure);
}
