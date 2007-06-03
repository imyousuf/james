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

package org.apache.james.experimental.imapserver.encode.base;

import java.util.Collection;
import java.util.Iterator;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.experimental.imapserver.encode.ImapEncoder;
import org.apache.james.experimental.imapserver.encode.ImapResponseComposer;

abstract public class AbstractChainedImapEncoder extends AbstractLogEnabled implements ImapEncoder {

    private final ImapEncoder next;
    
    public AbstractChainedImapEncoder(final ImapEncoder next) {
        super();
        this.next = next;
    }

    public void enableLogging(Logger logger) {
        super.enableLogging(logger);
        setupLogger(next);
    }
    
    public void encode(ImapMessage message, ImapResponseComposer composer) {
        final boolean isAcceptable = isAcceptable(message);
        if (isAcceptable) {
            doEncode(message, composer);
        } else {
            chainEncode(message, composer);
        }
    }

    protected void chainEncodeAll(final Collection messages, final ImapResponseComposer composer) {
        for (Iterator iter = messages.iterator(); iter.hasNext();) {
            ImapMessage message = (ImapMessage) iter.next();
            chainEncode(message, composer);
        }
    }
    
    protected void chainEncode(ImapMessage message, ImapResponseComposer composer) {
        next.encode(message, composer);
    }

    /**
     * Is the given message acceptable?
     * 
     * @param message
     *            <code>ImapMessage</code>, not null
     * @return true if the given message is encodable by this encoder
     */
    abstract protected boolean isAcceptable(final ImapMessage message);
    
    /**
     * Processes an acceptable message. Only messages passing
     * {@link #isAcceptable(ImapMessage)} should be passed to this method.
     * 
     * @param acceptableMessage
     *            <code>ImapMessage</code>, not null
     * @param composer <code>ImapResponseComposer</code>, not null
     */
    abstract protected void doEncode(ImapMessage acceptableMessage, ImapResponseComposer composer);
}
