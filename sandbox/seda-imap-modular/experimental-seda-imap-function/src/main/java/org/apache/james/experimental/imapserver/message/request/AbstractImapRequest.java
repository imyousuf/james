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
package org.apache.james.experimental.imapserver.message.request;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.message.ImapRequestMessage;
import org.apache.james.experimental.imapserver.message.ImapResponseMessage;
import org.apache.james.experimental.imapserver.processor.ImapRequestProcessor;

abstract public class AbstractImapRequest extends AbstractLogEnabled implements ImapRequestMessage {
    
	private final String tag;
    private final ImapCommand command;
    private final ImapRequestProcessor processor;
    
    public AbstractImapRequest(final String tag, final ImapCommand command, final ImapRequestProcessor processor) {
        this.tag = tag;
        this.command = command;
        this.processor = processor;
    }
    
	public void enableLogging(Logger logger) {
		super.enableLogging(logger);
		setupLogger(processor);
	}

	public final ImapCommand getCommand() {
		return command;
	}

	public final String getTag() {
		return tag;
	}

	public final ImapResponseMessage process(ImapSession session) {
    	final ImapResponseMessage result = processor.process(this, session);
    	return result;
    }
}
