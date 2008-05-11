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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.commons.collections.IteratorUtils;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;

public class MessageResultUtils {

    /**
     * Gets all header lines.
     * @param iterator {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>MessageResult.Header<code>'s,
     * in their natural order
     * 
     * @throws MessagingException
     */
    public static List getAll(final Iterator iterator) {
        List results = IteratorUtils.toList(iterator);
        return results;
    }
    
    /**
     * Gets header lines whose header names matches (ignoring case)
     * any of those given.
     * @param names header names to be matched, not null
     * @param iterator {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>MessageResult.Header</code>'s,
     * in their natural order
     * @throws MessagingException
     */
    public static List getMatching(final String[] names, final Iterator iterator) throws MessagingException {
        final List results = new ArrayList(20);
        if (iterator != null) {
            while(iterator.hasNext()) {
                MessageResult.Header header = (MessageResult.Header) iterator.next();
                final String headerName = header.getName();
                if (headerName != null) {
                    final int length = names.length;
                    for (int i=0;i<length;i++) {
                        final String name = names[i];
                        if (headerName.equalsIgnoreCase(name)) {
                            results.add(header);
                            break;
                        }
                    }
                }
            }
        }
        return results;
    }
    
    /**
     * Gets header lines whose header names matches (ignoring case)
     * any of those given.
     * @param names header names to be matched, not null
     * @param iterator {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>MessageResult.Header</code>'s,
     * in their natural order
     * @throws MessagingException
     */
    public static List getMatching(final Collection names, final Iterator iterator) throws MessagingException {
        final List result = matching(names, iterator, false);
        return result;
    }

    private static List matching(final Collection names, final Iterator iterator, boolean not) throws MailboxManagerException {
        final List results = new ArrayList(names.size());
        if (iterator != null) {
            while(iterator.hasNext()) {
                final MessageResult.Header header = (MessageResult.Header) iterator.next();
                final boolean match = contains(names, header);
                final boolean add = (not && !match) || (!not && match);
                if (add) {
                    results.add(header);
                }
            }
        }
        return results;
    }

    private static boolean contains(final Collection names, MessageResult.Header header) throws MailboxManagerException {
        boolean match = false;
        final String headerName = header.getName();
        if (headerName != null) {
            for (final Iterator it = names.iterator(); it.hasNext();) {
                final String name = (String) it.next();
                if (name.equalsIgnoreCase(headerName)) {
                    match = true;
                    break;
                }
            }
        }
        return match;
    }
    
    /**
     * Gets header lines whose header names matches (ignoring case)
     * any of those given.
     * @param names header names to be matched, not null
     * @param iterator {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>MessageResult.Header</code>'s,
     * in their natural order
     * @throws MessagingException
     */
    public static List getNotMatching(final Collection names, final Iterator iterator) throws MessagingException {
        final List result = matching(names, iterator, true);
        return result;
    }
    
    /**
     * Gets a header matching the given name.
     * The matching is case-insensitive.
     * @param name name to be matched, not null
     * @param iterator <code>Iterator</code> of <code>MessageResult.Header</code>'s,
     * not null
     * @return <code>MessageResult.Header</code>, 
     * or null if the header does not exist
     * @throws MessagingException 
     */
    public static MessageResult.Header getMatching(final String name, final Iterator iterator) throws MessagingException {
        MessageResult.Header result = null;
        if (name != null) {
            while(iterator.hasNext()) {
                MessageResult.Header header = (MessageResult.Header) iterator.next();
                final String headerName = header.getName();
                if (name.equalsIgnoreCase(headerName)) {
                    result = header;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Gets header lines whose header name fails to match (ignoring case)
     * all of the given names.
     * @param names header names, not null
     * @param iterator {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>@MessageResult.Header</code>'s,
     * in their natural order
     * @throws MessagingException
     */
    public static List getNotMatching(final String[] names, final Iterator iterator) throws MessagingException {
        final List results = new ArrayList(20);
        if (iterator != null) {
            while(iterator.hasNext()) {
                MessageResult.Header header = (MessageResult.Header) iterator.next();
                final String headerName = header.getName();
                if (headerName != null) {
                    final int length = names.length;
                    boolean match = false;
                    for (int i=0;i<length;i++) {
                        final String name = names[i];
                        if (headerName.equalsIgnoreCase(name)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        results.add(header);
                    }
                }
            }
        }
        return results;
    }
    
    /**
     * Is the given datum included in these results?
     * @param message <code>MessageResult</code>, possibly null
     * @param datum {@link MessageResult} datum constant
     * @return true if <code>MessageResult</code> includes 
     * the given datum, false if <code>MessageResult</code> is null
     * or does not contain this datum
     */
    public static boolean isIncluded(final MessageResult message, final int datum) {
        final boolean result;
        if (message == null) {
            result = false;
        } else if (datum == FetchGroup.MINIMAL) {
            result = true;
        } else {
            final int includes = message.getIncludedResults().content();
            result = (includes & datum) == datum;
        }
        return result;
    }
    
    /**
     * Is {@link FetchGroup#BODY_CONTENT} included in these results?
     * @param message <code>MessageResult</code>, possibly null
     * @return true if <code>MessageResult</code> includes 
     * BODY_CONTENT, false if <code>MessageResult</code> is null
     * or does not contain BODY_CONTENT
     * @see #isIncluded(MessageResult, int)
     */
    public static boolean isBodyContentIncluded(final MessageResult message) {
        return isIncluded(message, FetchGroup.BODY_CONTENT);
    }
    
    /**
     * Is {@link FetchGroup#FLAGS} included in these results?
     * @param message <code>MessageResult</code>, possibly null
     * @return true if <code>MessageResult</code> includes 
     * FLAGS, false if <code>MessageResult</code> is null
     * or does not contain FLAGS
     * @see #isIncluded(MessageResult, int)
     */
    public static boolean isFlagsIncluded(final MessageResult message) {
        return isIncluded(message, FetchGroup.FLAGS);
    }
    
    /**
     * Is {@link FetchGroup#FULL_CONTENT} included in these results?
     * @param message <code>MessageResult</code>, possibly null
     * @return true if <code>MessageResult</code> includes 
     * FULL_CONTENT, false if <code>MessageResult</code> is null
     * or does not contain FULL_CONTENT
     * @see #isIncluded(MessageResult, int)
     */
    public static boolean isFullContentIncluded(final MessageResult message) {
        return isIncluded(message, FetchGroup.FULL_CONTENT);
    }
    
    /**
     * Is {@link FetchGroup#HEADERS} included in these results?
     * @param message <code>MessageResult</code>, possibly null
     * @return true if <code>MessageResult</code> includes 
     * HEADERS, false if <code>MessageResult</code> is null
     * or does not contain HEADERS
     * @see #isIncluded(MessageResult, int)
     */
    public static boolean isHeadersIncluded(final MessageResult message) {
        return isIncluded(message, FetchGroup.HEADERS);
    }
    
    /**
     * Is {@link FetchGroup#INTERNAL_DATE} included in these results?
     * @param message <code>MessageResult</code>, possibly null
     * @return true if <code>MessageResult</code> includes 
     * INTERNAL_DATE, false if <code>MessageResult</code> is null
     * or does not contain INTERNAL_DATE
     * @see #isIncluded(MessageResult, int)
     */
    public static boolean isInternalDateIncluded(final MessageResult message) {
        return isIncluded(message, FetchGroup.INTERNAL_DATE);
    }
    
    /**
     * Is {@link FetchGroup#MIME_MESSAGE} included in these results?
     * @param message <code>MessageResult</code>, possibly null
     * @return true if <code>MessageResult</code> includes 
     * MIME_MESSAGE, false if <code>MessageResult</code> is null
     * or does not contain MIME_MESSAGE
     * @see #isIncluded(MessageResult, int)
     */
    public static boolean isMimeMessageIncluded(final MessageResult message) {
        return isIncluded(message, FetchGroup.MIME_MESSAGE);
    }   
    
    /**
     * Is {@link FetchGroup#SIZE} included in these results?
     * @param message <code>MessageResult</code>, possibly null
     * @return true if <code>MessageResult</code> includes 
     * SIZE, false if <code>MessageResult</code> is null
     * or does not contain SIZE
     * @see #isIncluded(MessageResult, int)
     */
    public static boolean isSizeIncluded(final MessageResult message) {
        return isIncluded(message, FetchGroup.SIZE);
    }
}
