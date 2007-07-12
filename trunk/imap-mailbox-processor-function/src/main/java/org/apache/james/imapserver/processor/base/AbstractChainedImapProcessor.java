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

package org.apache.james.imapserver.processor.base;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;

abstract public class AbstractChainedImapProcessor extends AbstractLogEnabled
        implements ImapProcessor {

    private final ImapProcessor next;

    /**
     * Constructs a chainable <code>ImapProcessor</code>.
     * 
     * @param next
     *            next <code>ImapProcessor</code> in the chain, not null
     */
    public AbstractChainedImapProcessor(final ImapProcessor next) {
        this.next = next;
    }

    public void enableLogging(Logger logger) {
        super.enableLogging(logger);
        setupLogger(next);
    }

    public ImapResponseMessage process(final ImapMessage message,
            final ImapSession session) {
        final ImapResponseMessage result;
        final boolean isAcceptable = isAcceptable(message);
        if (isAcceptable) {
            result = doProcess(message, session);
        } else {
            result = next.process(message, session);
        }
        return result;
    }

    /**
     * Is the given message acceptable?
     * 
     * @param message
     *            <code>ImapMessage</code>, not null
     * @return true if the given message is processable by this processable
     */
    abstract protected boolean isAcceptable(final ImapMessage message);

    /**
     * Processes an acceptable message. Only messages passing
     * {@link #isAcceptable(ImapMessage)} should be passed to this method.
     * 
     * @param acceptableMessage
     *            <code>ImapMessage</code>, not null
     * @param session
     *            <code>ImapSession</code>, not null
     * @return <code>ImapResponseMessage</code>, not null
     */
    abstract protected ImapResponseMessage doProcess(
            final ImapMessage acceptableMessage, final ImapSession session);
}
