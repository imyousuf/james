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

/**
 * 
 */
package org.apache.james.imapserver.processor.imap4rev1.fetch;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import org.apache.james.imap.message.response.imap4rev1.FetchResponse.BodyElement;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResult.Content;

final class ContentBodyElement implements BodyElement {
    private final String name;
    private final MessageResult.Content content;
    
    public ContentBodyElement(final String name, final Content content) {
        super();
        this.name = name;
        this.content = content;
    }

    /**
     * @see org.apache.james.imap.message.response.imap4rev1.FetchResponse.BodyElement#getName()
     */
    public String getName() {
        return name;
    }
    
    /**
     * @see org.apache.james.imap.message.response.imap4rev1.FetchResponse.BodyElement#size()
     */
    public long size() {
        return content.size();
    }
    
    /**
     * @see org.apache.james.imap.message.response.imap4rev1.FetchResponse.BodyElement#writeTo(WritableByteChannel)
     */
    public void writeTo(WritableByteChannel channel) throws IOException {
        content.writeTo(channel);
    }
}