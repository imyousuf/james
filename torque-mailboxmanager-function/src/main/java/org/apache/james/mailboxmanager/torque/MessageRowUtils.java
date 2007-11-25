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

package org.apache.james.mailboxmanager.torque;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResult.Content;
import org.apache.james.mailboxmanager.impl.MessageFlags;
import org.apache.james.mailboxmanager.impl.MessageResultImpl;
import org.apache.james.mailboxmanager.torque.om.MessageBody;
import org.apache.james.mailboxmanager.torque.om.MessageHeader;
import org.apache.james.mailboxmanager.torque.om.MessageRow;
import org.apache.james.mailboxmanager.util.UidToKeyConverter;
import org.apache.torque.TorqueException;

public class MessageRowUtils {

    public static final byte[] BYTES_NEW_LINE = {0x0D, 0x0A};
    public static final byte[] BYTES_HEADER_FIELD_VALUE_SEP = {0x3A, 0x20};
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    
    private static final class Header implements MessageResult.Header, MessageResult.Content {
            private final String name;
            private final String value;
            private final long size;
            
            public Header(final MessageHeader header) {
                this.name = header.getField();
                this.value = header.getValue();
                size = name.length() + value.length() + 2;
            }
            
            public Content getContent() throws MessagingException {
                return this;
            }
    
            public String getName() throws MailboxManagerException {
                return name;
            }
    
            public String getValue() throws MailboxManagerException {
                return value;
            }
    
            public long size() {
                return size;
            }
    
            public void writeTo(StringBuffer buffer) {
    // TODO: sort out encoding
                for (int i=0; i<name.length();i++) {
                    buffer.append((char)(byte) name.charAt(i));
                }
                buffer.append(':');
                buffer.append(' ');
                for (int i=0; i<value.length();i++) {
                    buffer.append((char)(byte) value.charAt(i));
                }
            }

            public void writeTo(WritableByteChannel channel) throws IOException {
                writeAll(channel, US_ASCII.encode(name));
                ByteBuffer buffer = ByteBuffer.wrap(BYTES_HEADER_FIELD_VALUE_SEP);
                writeAll(channel, buffer);
                writeAll(channel, US_ASCII.encode(value));
            }
            
            private void writeAll(WritableByteChannel channel, ByteBuffer buffer) throws IOException {
                while (channel.write(buffer) > 0) {
                    // write more
                }
            }
        }

    private final static class ByteContent implements MessageResult.Content {
    
        private final byte[] contents;
        private final long size;
        public ByteContent(final byte[] contents) {
            this.contents = contents;
            size = contents.length + MessageUtils.countUnnormalLines(contents);
        }
        
        public long size() {
            return size;
        }
        
        public void writeTo(StringBuffer buffer) {
            MessageUtils.normalisedWriteTo(contents, buffer);
        }

        public void writeTo(WritableByteChannel channel) throws IOException {
            ByteBuffer buffer = ByteBuffer.wrap(contents);
            while (channel.write(buffer) > 0) {
                // write more
            }
        }
    }

    private final static class FullContent implements MessageResult.Content {
        private final byte[] contents;
        private final List headers;
        private final long size;
        
        public FullContent(final byte[] contents, final List headers) throws MailboxManagerException {
            this.contents =  contents;
            this.headers = headers;
            this.size = caculateSize();
        }
    
        private long caculateSize() {
            long result = contents.length + MessageUtils.countUnnormalLines(contents);
            result += 2;
            for (final Iterator it=headers.iterator(); it.hasNext();) {
                final MessageResult.Header header = (MessageResult.Header) it.next();
                if (header != null) {
                    result += header.size();
                    result += 2;
                }
            }
            return result;
        }
    
        public void writeTo(StringBuffer buffer) {
            for (final Iterator it=headers.iterator(); it.hasNext();) {
                final MessageResult.Header header = (MessageResult.Header) it.next();
                if (header != null) {
                    header.writeTo(buffer);
                }
                buffer.append('\r');
                buffer.append('\n');
            }
            buffer.append('\r');
            buffer.append('\n');
            MessageUtils.normalisedWriteTo(contents, buffer);
        }
    
        public long size() {
            return size;
        }

        public void writeTo(WritableByteChannel channel) throws IOException {
            ByteBuffer newLine = ByteBuffer.wrap(BYTES_NEW_LINE);
            for (final Iterator it=headers.iterator(); it.hasNext();) {
                final MessageResult.Header header = (MessageResult.Header) it.next();
                if (header != null) {
                    header.writeTo(channel);
                }
                newLine.rewind();
                writeAll(channel, newLine);
            }
            newLine.rewind();
            writeAll(channel, newLine);
            final ByteBuffer wrap = ByteBuffer.wrap(contents);
            writeAll(channel, wrap);
        }

        private void writeAll(WritableByteChannel channel, ByteBuffer buffer) throws IOException {
            while (channel.write(buffer) > 0) {
                // write more
            }
        }
    }

    /**
     * Converts {@link MessageRow} to {@link MessageFlags}.
     * @param messageRows <code>Collectio</code> of {@link MessageRow},
     * not null 
     * @return <code>MessageFlags</code>, not null
     * @throws TorqueException
     */
    public static MessageFlags[] toMessageFlags(final Collection messageRows) throws TorqueException {
        final MessageFlags[] results = new MessageFlags[messageRows.size()];
        int i=0;
        for (final Iterator it=messageRows.iterator(); it.hasNext();) {
            MessageRow row = (MessageRow) it.next();
            final Flags flags = row.getMessageFlags().getFlagsObject();
            final long uid = row.getUid();
            results[i++] = new MessageFlags(uid, flags);
        }
        return results;
    }

    public static List createHeaders(MessageRow messageRow) throws TorqueException {
        final List headers=messageRow.getMessageHeaders();
        Collections.sort(headers, new Comparator() {
    
            public int compare(Object one, Object two) {
                return ((MessageHeader) one).getLineNumber() - ((MessageHeader)two).getLineNumber();
            }
            
        });
        
        final List results = new ArrayList(headers.size());
        for (Iterator it=headers.iterator();it.hasNext();) {
            final MessageHeader messageHeader = (MessageHeader) it.next();
            final MessageRowUtils.Header header = new MessageRowUtils.Header(messageHeader);
            results.add(header);
        }
        return results;
    }

    public static Content createBodyContent(MessageRow messageRow) throws TorqueException {
        final MessageBody body = (MessageBody) messageRow.getMessageBodys().get(0);
        final byte[] bytes = body.getBody();
        final MessageRowUtils.ByteContent result = new MessageRowUtils.ByteContent(bytes);
        return result;
    }

    public static Content createFullContent(final MessageRow messageRow, List headers) throws TorqueException, MailboxManagerException {
        if (headers == null) {
            headers = createHeaders(messageRow);
        }
        final MessageBody body = (MessageBody) messageRow.getMessageBodys().get(0);
        final byte[] bytes = body.getBody();
        final MessageRowUtils.FullContent results = new MessageRowUtils.FullContent(bytes, headers);
        return results;
    }

    public static MessageResult loadMessageResult(MessageRow messageRow, int result, UidToKeyConverter uidToKeyConverter)
            throws TorqueException, MailboxManagerException {
        MessageResultImpl messageResult = new MessageResultImpl();
        if ((result & MessageResult.MIME_MESSAGE) > 0) {
            messageResult.setMimeMessage(TorqueMimeMessage.createMessage(messageRow));
            result -= MessageResult.MIME_MESSAGE;
        }
        if ((result & MessageResult.UID) > 0) {
            messageResult.setUid(messageRow.getUid());
            result -= MessageResult.UID;
        }
        if ((result & MessageResult.FLAGS) > 0) {
            org.apache.james.mailboxmanager.torque.om.MessageFlags messageFlags
                = messageRow.getMessageFlags();
            if (messageFlags!=null) {
                messageResult.setFlags(messageFlags.getFlagsObject());  
            }
            result -= MessageResult.FLAGS;
        }
        if ((result & MessageResult.SIZE) > 0) {
            messageResult.setSize(messageRow.getSize());
            result -= MessageResult.SIZE;
        }
        if ((result & MessageResult.INTERNAL_DATE) > 0) {
            messageResult.setInternalDate(messageRow.getInternalDate());
            result -= MessageResult.INTERNAL_DATE;
        }
        if ((result & MessageResult.KEY) > 0) {
            messageResult.setKey(uidToKeyConverter.toKey(messageRow.getUid()));
            result -= MessageResult.KEY;
        }
        if ((result & MessageResult.HEADERS) > 0) {
            messageResult.setHeaders(createHeaders(messageRow));
            result -= MessageResult.HEADERS;
        }
        if ((result & MessageResult.BODY_CONTENT) > 0) {
            messageResult.setMessageBody(createBodyContent(messageRow));
            result -= MessageResult.BODY_CONTENT;
        }
        if ((result & MessageResult.FULL_CONTENT) > 0) {
            messageResult.setFullMessage(createFullContent(messageRow, messageResult.getHeaders()));
            result -= MessageResult.FULL_CONTENT;
        }
        if ((result & MessageResult.MSN) > 0) {
            // ATM implemented by wrappers
            result -= MessageResult.MSN;
        }
        if (result != 0) {
            throw new RuntimeException("Unsupported result: " + result);
        }
        
        return messageResult;
    }
    
    /**
     * Gets a comparator that evaluates {@link MessageRow}'s
     * on the basis of their UIDs.
     * @return {@link Comparator}, not null
     */
    public static Comparator getUidComparator() {
        return UidComparator.INSTANCE;
    }
    
    private static final class UidComparator implements Comparator {
        private static final UidComparator INSTANCE = new UidComparator();
        
        public int compare(Object one, Object two) {
            final MessageRow rowOne = (MessageRow) one;
            final MessageRow rowTwo = (MessageRow) two;
            final int result = (int)(rowOne.getUid() - rowTwo.getUid());
            return result;
        }
        
    }
}
