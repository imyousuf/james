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

package org.apache.james.imap.message.response.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.message.response.ImapResponseMessage;

abstract public class AbstractImapResponse extends AbstractLogEnabled implements ImapResponseMessage {

    private final ImapCommand command;
    private final String tag;
    private final List unsolicatedResponses;
    
    public AbstractImapResponse(final ImapCommand command, final String tag) {
        super();
        this.command = command;
        this.tag = tag;
        unsolicatedResponses = new ArrayList();
    }
    
    public final String getTag() {
        return tag;
    }

    public ImapCommand getCommand() {
        return command;
    }
    
    public void addUnsolicitedResponses(List responses) {
        unsolicatedResponses.addAll(responses);
    }
    
    public void addUnsolicitedResponses(ImapResponseMessage response) {
        unsolicatedResponses.add(response);
    }
    
    public List getUnsolicatedResponses() {
        return Collections.unmodifiableList(unsolicatedResponses);
    }
}
