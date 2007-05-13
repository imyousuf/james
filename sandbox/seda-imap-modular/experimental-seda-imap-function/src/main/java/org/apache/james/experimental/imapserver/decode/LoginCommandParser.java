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
package org.apache.james.experimental.imapserver.decode;

import org.apache.james.experimental.imapserver.ImapRequestLineReader;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.commands.ImapCommandFactory;
import org.apache.james.experimental.imapserver.message.ImapRequestMessage;

class LoginCommandParser extends AbstractImapCommandParser  implements InitialisableCommandFactory {

    public LoginCommandParser() {
    }

    /**
     * @see org.apache.james.experimental.imapserver.decode.InitialisableCommandFactory#init(org.apache.james.experimental.imapserver.commands.ImapCommandFactory)
     */
    public void init(ImapCommandFactory factory)
    {
        final ImapCommand command = factory.getLogin();
        setCommand(command);
    }
    
    protected ImapRequestMessage decode(ImapCommand command, ImapRequestLineReader request, String tag) throws ProtocolException {
        final String userid = astring( request );
        final String password = astring( request );
        endLine( request );
        final ImapRequestMessage result = getMessageFactory().createLoginMessage(command, userid, password, tag);
        return result;
    }
    
}
