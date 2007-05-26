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

import java.io.ByteArrayInputStream;
import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.james.experimental.imapserver.ImapRequestLineReader;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.experimental.imapserver.message.ImapMessage;
import org.apache.james.experimental.imapserver.message.ImapMessageFactory;

class AppendCommandParser extends AbstractImapCommandParser implements InitialisableCommandFactory {        

    public AppendCommandParser() {
    }
    
    /**
     * @see org.apache.james.experimental.imapserver.decode.InitialisableCommandFactory#init(org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory)
     */
    public void init(Imap4Rev1CommandFactory factory)
    {
        final ImapCommand command = factory.getAppend();
        setCommand(command);
    }

    /**
     * If the next character in the request is a '(', tries to read
     * a "flag_list" argument from the request. If not, returns a
     * MessageFlags with no flags set.
     */
    public Flags optionalAppendFlags( ImapRequestLineReader request )
            throws ProtocolException
    {
        char next = request.nextWordChar();
        if ( next == '(' ) {
            return flagList( request );
        }
        else {
            return null;
        }
    }

    /**
     * If the next character in the request is a '"', tries to read
     * a DateTime argument. If not, returns null.
     */
    public Date optionalDateTime( ImapRequestLineReader request )
            throws ProtocolException
    {
        char next = request.nextWordChar();
        if ( next == '"' ) {
            return dateTime( request );
        }
        else {
            return null;
        }
    }

    /**
     * Reads a MimeMessage encoded as a string literal from the request.
     * TODO shouldn't need to read as a string and write out bytes
     *      use FixedLengthInputStream instead. Hopefully it can then be dynamic.
     * @param request The Imap APPEND request
     * @return A MimeMessage read off the request.
     */
    public MimeMessage mimeMessage( ImapRequestLineReader request )
            throws ProtocolException
    {
        request.nextWordChar();
        String mailString = consumeLiteral(request);
        MimeMessage mm = null;

        try {
            byte[] messageBytes = mailString.getBytes("US-ASCII");
            mm = new MimeMessage(null, new ByteArrayInputStream(
                    messageBytes));
        } catch (Exception e) {
            throw new ProtocolException("UnexpectedException: "
                    + e.getMessage(), e);

        }
        return mm;
    }

    protected ImapMessage decode(ImapCommand command, ImapRequestLineReader request, String tag) throws ProtocolException {
        String mailboxName = mailbox( request );
        Flags flags = optionalAppendFlags( request );
        if ( flags == null ) {
            flags = new Flags();
        }
        Date datetime = optionalDateTime( request );
        if ( datetime == null ) {
            datetime = new Date();
        }
        MimeMessage message = mimeMessage( request );
        endLine( request );
        final ImapMessageFactory factory = getMessageFactory();
        final ImapMessage result = factory.createAppendMessage(command, mailboxName, 
                flags, datetime, message, tag);
        return result;
    }
}
