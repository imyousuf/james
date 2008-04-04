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

package org.apache.james.experimental.imapserver;

import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.imap.command.imap4rev1.StandardImap4Rev1CommandFactory;
import org.apache.james.imap.message.request.base.BaseImap4Rev1MessageFactory;
import org.apache.james.imap.message.response.imap4rev1.status.UnpooledStatusResponseFactory;
import org.apache.james.imapserver.codec.decode.ImapCommandParserFactory;
import org.apache.james.imapserver.codec.decode.ImapDecoder;
import org.apache.james.imapserver.codec.decode.ImapDecoderFactory;
import org.apache.james.imapserver.codec.decode.imap4rev1.Imap4Rev1CommandParserFactory;
import org.apache.james.imapserver.codec.decode.main.DefaultImapDecoder;

/**
 * TODO: this is temporary: should let the container do the coupling.
 */
public class DefaultImapDecoderFactory implements ImapDecoderFactory{
    
    
    
    public static final ImapDecoder createDecoder() {
        final UnpooledStatusResponseFactory unpooledStatusResponseFactory = new UnpooledStatusResponseFactory();
        final Imap4Rev1MessageFactory messageFactory = new BaseImap4Rev1MessageFactory(unpooledStatusResponseFactory);
        final Imap4Rev1CommandFactory commandFactory = new StandardImap4Rev1CommandFactory();
        final ImapCommandParserFactory imapCommands = new Imap4Rev1CommandParserFactory(
                messageFactory, commandFactory, unpooledStatusResponseFactory);
        final ImapDecoder result = new DefaultImapDecoder(messageFactory,
                imapCommands);
        return result;
    }

    public ImapDecoder buildImapDecoder() {
        return createDecoder();
    }
    
    
}
