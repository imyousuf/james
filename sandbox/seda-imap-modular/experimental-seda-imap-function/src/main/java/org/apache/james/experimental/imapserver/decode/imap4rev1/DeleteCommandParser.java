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
package org.apache.james.experimental.imapserver.decode.imap4rev1;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.experimental.imapserver.decode.ImapRequestLineReader;
import org.apache.james.experimental.imapserver.decode.InitialisableCommandFactory;
import org.apache.james.experimental.imapserver.decode.base.AbstractImapCommandParser;

class DeleteCommandParser extends AbstractImapCommandParser  implements InitialisableCommandFactory {

    public DeleteCommandParser() {
    }

    /**
     * @see org.apache.james.experimental.imapserver.decode.InitialisableCommandFactory#init(org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory)
     */
    public void init(Imap4Rev1CommandFactory factory)
    {
        final ImapCommand command = factory.getDelete();
        setCommand(command);
    }
    
    protected ImapMessage decode(ImapCommand command, ImapRequestLineReader request, String tag) throws ProtocolException {
        String mailboxName = mailbox( request );
        endLine( request );
        final Imap4Rev1MessageFactory factory = getMessageFactory();
        final ImapMessage result = factory.createDeleteMessage( command, mailboxName, tag );
        return result;
    }
    
}
