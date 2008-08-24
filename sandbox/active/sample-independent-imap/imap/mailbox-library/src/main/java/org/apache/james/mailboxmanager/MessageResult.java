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

package org.apache.james.mailboxmanager;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.MessagingException;

import org.apache.james.mailboxmanager.util.MessageResultUtils;

/**
 * <p>
 * Used to get specific informations about a Message without dealing with a
 * MimeMessage instance. Demanded information can be requested by binary
 * combining the constants.
 * </p>
 * 
 * <p>
 * I came to the Idea of the MessageResult because there are many possible
 * combinations of different requests (uid, msn, MimeMessage, Flags).
 * </p>
 * <p>
 * e.g. I want to have all uids, msns and flags of all messages. (a common IMAP
 * operation) Javamail would do it that way:
 * <ol>
 * <li>get all Message objects (Message[])</li>
 * <li>call Message.getMessageNumber() </li>
 * <li>call Message.getFlags() </li>
 * <li>call Folder.getUid(Message)</li>
 * </ol>
 * <p>
 * This means creating a lazy-loading MimeMessage instance. </br> So why don't
 * call getMessages(MessageResult.UID | MessageResult.MSN |
 * MessageResult.FLAGS)? This would leave a lot of room for the implementation
 * to optimize
 * </p>
 * 
 * 
 */

public interface MessageResult extends Comparable, Headers {

    /**
     * Indicates the results fetched.
     */
    public interface FetchGroup {
        
        /**
         * For example: could have best performance when doing store and then
         * forget. UIDs are always returned
         */
        public static final int MINIMAL = 0x00;
        /**
         * 
         */
        public static final int MIME_DESCRIPTOR = 0x01;

        public static final int SIZE = 0x20;
        public static final int INTERNAL_DATE = 0x40;
        public static final int FLAGS = 0x80;
        public static final int HEADERS = 0x100;
        public static final int FULL_CONTENT = 0x200;
        public static final int BODY_CONTENT = 0x400;
        public static final int MIME_HEADERS = 0x800;
        public static final int MIME_CONTENT = 0x1000;
        
        /**
         * Contents to be fetched.
         * Composed bitwise.
         * 
         * @return bitwise descripion
         * @see #MINIMAL
         * @see #MIME_MESSAGE
         * @see #KEY
         * @see #SIZE
         * @see #INTERNAL_DATE
         * @see #FLAGS
         * @see #HEADERS
         * @see #FULL_CONTENT
         * @see #BODY_CONTENT
         * @see #MIME_CONTENT
         */
        public int content();
        
        /**
         * Gets contents to be fetched for contained parts.
         * For each part to be contained, 
         * only one descriptor should be contained.
         * @return <code>Set</code> of {@link PartContentDescriptor},
         * or null if there is no part content to be fetched
         */
        public Set getPartContentDescriptors();
        
        /**
         * Describes the contents to be fetched for a mail part.
         * All implementations MUST implement equals.
         * Two implementations are equal if and only if
         * their paths are equal.
         */
        public interface PartContentDescriptor {
            /**
             * Contents to be fetched.
             * Composed bitwise.
             * 
             * @return bitwise descripion
             * @see #MINIMAL
             * @see #MIME_MESSAGE
             * @see #KEY
             * @see #SIZE
             * @see #INTERNAL_DATE
             * @see #FLAGS
             * @see #HEADERS
             * @see #FULL_CONTENT
             * @see #BODY_CONTENT
             */
            public int content();
            
            /**
             * Path describing the part to be fetched.
             * @return path describing the part, not null
             */
            public MimePath path();
        }
    }
    
    /**
     * Gets the results set.
     * @return bitwise indication of result set
     * @see MessageResultUtils#isIncluded(MessageResult, int)
     */
    FetchGroup getIncludedResults();

    MimeDescriptor getMimeDescriptor() throws MailboxManagerException;

    long getUid();

    long getUidValidity();

    /**
     * 
     * <p>
     * IMAP defines this as the time when the message has arrived to the server
     * (by smtp). Clients are also allowed to set the internalDate on apppend.
     * </p>
     * <p>
     * Is this Mail.getLastUpdates() for James delivery? Should we use
     * MimeMessage.getReceivedDate()?
     * </p>
     * 
     */

    Date getInternalDate();

    /**
     * TODO optional, to be decided <br />
     * maybe this is a good thing because IMAP often requests only the Flags and
     * this way we don't need to create a lazy-loading MimeMessage instance just
     * for the Flags.
     * 
     */
    Flags getFlags() throws MailboxManagerException;

    int getSize();
    
    /**
     * Gets headers for the message.
     * @return <code>Header</code> <code>Iterator</code>, 
     * or null if {@link FetchGroup#HEADERS} was not fetched
     */
    Iterator headers() throws MailboxManagerException;
    
    /**
     * Iterates the message headers for the given
     * part in a multipart message.
     * @param path describing the part's position within
     * a multipart message
     * @return  <code>Header</code> <code>Iterator</code>, 
     * or null when {@link FetchGroup#mimeHeaders()} does not
     * include the index and when the mime part cannot be found
     * @throws MailboxManagerException
     */
    Iterator iterateHeaders(MimePath path) throws MailboxManagerException;
    
    /**
     * Iterates the MIME headers for the given
     * part in a multipart message.
     * @param path describing the part's position within
     * a multipart message
     * @return  <code>Header</code> <code>Iterator</code>, 
     * or null when {@link FetchGroup#mimeHeaders()} does not
     * include the index and when the mime part cannot be found
     * @throws MailboxManagerException
     */
    Iterator iterateMimeHeaders(MimePath path) throws MailboxManagerException;
    
    /**
     * A header.
     */
    public interface Header extends Content {
        
        /**
         * Gets the name of this header.
         * @return name of this header
         * @throws MessagingException 
         */
        public String getName() throws MailboxManagerException;
        
        /**
         * Gets the (unparsed) value of this header.
         * @return value of this header
         * @throws MessagingException
         */
        public String getValue() throws MailboxManagerException;
    }
    
    /**
     * Gets the full message including headers and body.
     * The message data should have normalised line endings (CRLF).
     * @return <code>Content</code>, 
     * or or null if {@link FetchGroup#FULL_CONTENT} has not been included in the 
     * results
     */
    Content getFullContent() throws MailboxManagerException;


    /**
     * Gets the full content of the given mime part.
     * @param path describes the part
     * @return <code>Content</code>,
     * or null when {@link FetchGroup#mimeBodies()} did not been include 
     * the given index and when the mime part cannot be found
     * @throws MailboxManagerException
     */
    Content getFullContent(MimePath path) throws MailboxManagerException;
    
    /**
     * Gets the body of the message excluding headers.
     * The message data should have normalised line endings (CRLF).
     * @return <code>Content</code>,
     * or or null if {@link FetchGroup#FULL_CONTENT} has not been included in the 
     * results 
     */
    Content getBody() throws MailboxManagerException;
    
    /**
     * Gets the body of the given mime part.
     * @param path describes the part
     * @return <code>Content</code>,
     * or null when {@link FetchGroup#mimeBodies()} did not been include 
     * the given index and when the mime part cannot be found
     * @throws MailboxManagerException
     */
    Content getBody(MimePath path) throws MailboxManagerException;
    

    /**
     * Gets the body of the given mime part.
     * @param path describes the part
     * @return <code>Content</code>,
     * or null when {@link FetchGroup#mimeBodies()} did not been include 
     * the given index and when the mime part cannot be found
     * @throws MailboxManagerException
     */
    Content getMimeBody(MimePath path) throws MailboxManagerException;

    /**
     * IMAP needs to know the size of the content before it starts to write it out.
     * This interface allows direct writing whilst exposing total size.
     */
    public interface Content {
        /**
         * Writes content into the given buffer.
         * @param buffer <code>StringBuffer</code>, not null
         * @throws MessagingException
         */
        public void writeTo(StringBuffer buffer);
        
        /**
         * Writes content to the given channel.
         * @param channel <code>Channel</code> open, not null
         * @throws MailboxManagerException
         * @throws IOException when channel IO fails
         */
        public void writeTo(WritableByteChannel channel) throws IOException;
        
        /**
         * Size (in octets) of the content.
         * @return number of octets to be written
         * @throws MessagingException
         */
        public long size();
    }
    
    /**
     * Describes a path within a multipart MIME message.
     * All implementations must implement equals.
     * Two paths are equal if and only if
     * each position is identical.
     */
    public interface MimePath {
        
        /**
         * Gets the positions of each part in the path.
         * @return part positions describing the path
         */
        public int[] getPositions();
    }
    
    public interface MimeDescriptor extends Headers {

        /**
         * Gets the top level MIME content media type.
         * @return top level MIME content media type,
         * or null if default 
         */
        public String getMimeType();
        
        /**
         * Gets the MIME content subtype.
         * @return the MIME content subtype,
         * or null if default
         */
        public String getMimeSubType();
        
        /**
         * Gets the MIME <code>Content-ID</code> header value.
         * @return MIME <code>Content-ID</code>,
         * possibly null
         */
        public String getContentID();
        
        /**
         * Gets MIME <code>Content-Description</code> header value.
         * @return MIME <code>Content-Description</code>,
         * possibly null
         */
        public String getContentDescription();
        
        /**
         * Gets MIME <code>Content-Location</code> header value.
         * @return parsed MIME <code>Content-Location</code>, 
         * possibly null
         */
        public String getContentLocation();
        
        /**
         * Gets MIME <code>Content-MD5</code> header value.
         * @return parsed MIME <code>Content-MD5</code>, 
         * possibly null
         */
        public String getContentMD5();
        
        /**
         * Gets the MIME content transfer encoding.
         * @return MIME <code>Content-Transfer-Encoding</code>,
         * possibly null
         */
        public String getTransferContentEncoding();
        
        /**
         * Gets the languages, 
         * From the MIME <code>Content-Language</code> header value.
         * @return <code>List</code> of <code>String</code> names
         */
        public List getLanguages();
        
        /**
         * Gets MIME <code>Content-Disposition</code>.
         * @return <code>Content-Disposition</code>,
         * or null if no disposition header exists
         */
        public String getDisposition();
        
        /**
         * Gets MIME <code>Content-Disposition</code> 
         * parameters.
         * @return <code>Content-Disposition</code>
         * values indexed by names
         */
        public Map getDispositionParams();
        
        /**
         * Gets the number of lines of text in a part
         * of type <code>TEXT</code> when transfer
         * encoded.
         * 
         * @return <code>CRLF</code> count
         * when a <code>TEXT</code> type, otherwise -1
         */
        public long getLines();
        
        /**
         * The number of octets contained in the body of this part.
         * 
         * @return number of octets
         */
        public long getBodyOctets();
        
        /**
         * Gets parts.
         * @return <code>MimeDescriptor</code> <code>Iterator</code> 
         * when a composite top level MIME media type,
         * null otherwise
         */
        public Iterator parts();
        
        /**
         * Gets embedded message.
         * @return <code>MimeDescriptor</code> when top level MIME type is <code>message</code>, 
         * null otherwise
         */
        public MimeDescriptor embeddedMessage();
        
        /**
         * Gets headers.
         * @return <code>Header</code> <code>Iterator</code>,
         * not null 
         */
        public Iterator headers();
        
        /**
         * Gets MIME body parameters parsed from <code>Content-Type</code>.
         * @return <code>Header</code> <code>Iterator</code>,
         * not null
         */
        public Iterator contentTypeParameters();
    }
}
