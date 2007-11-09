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
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.commons.collections.IteratorUtils;

public class MessageResultUtils {

    /**
     * Gets all header lines.
     * @param iterator {@link MessageResult.Header} <code>Iterator</code>
     * @return <code>List</code> of <code>MessageResult.Header<code>'s,
     * in their natural order
     * 
     * @throws MessagingException
     */
    public static List getAll(final Iterator iterator) throws MessagingException {
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
}
